/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.aafwu00.evcache.client.spring.cloud;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
@Testcontainers
@Tags({@Tag("integration"), @Tag("docker")})
class EVCacheClientSpringCloudIntegrationTest {
    @Container
    static final GenericContainer MEMCACHED = new GenericContainer<>("memcached:alpine").withExposedPorts(11211);

    @Autowired
    private TodoApp.TodoRepository repository;
    @Autowired
    private MemcachedClient client;

    @DynamicPropertySource
    static void evcacheProperties(final DynamicPropertyRegistry registry) {
        registry.add("evcache.port", MEMCACHED::getFirstMappedPort);
    }

    @Test
    void cached() throws ExecutionException, InterruptedException {
        assertThat(repository.findAll()).isEqualTo(client.asyncGet("todos:findAll").get());
    }

    @SpringBootApplication
    @EnableCaching
    @EnableDiscoveryClient
    static class TodoApp {
        private static final String HOSTNAME = "localhost";
        private static final String IP_ADDRESS = "127.0.0.1";

        @Bean
        public ApplicationInfoManager applicationInfoManager() {
            final ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
            doReturn(InstanceInfo.Builder.newBuilder()
                                         .setAppName("test")
                                         .setStatus(InstanceInfo.InstanceStatus.UP)
                                         .setHostName(HOSTNAME)
                                         .setIPAddr(IP_ADDRESS)
                                         .build())
                .when(applicationInfoManager).getInfo();
            return applicationInfoManager;
        }

        @Bean
        public DiscoveryClient eurekaClient(final ConfigurableEnvironment environment) {
            final Application application = new Application("TODO");
            final AmazonInfo amazonInfo = AmazonInfo.Builder.newBuilder()
                                                            .addMetadata(AmazonInfo.MetaDataKey.availabilityZone,
                                                                         "DEFAULT")
                                                            .addMetadata(AmazonInfo.MetaDataKey.publicHostname,
                                                                         HOSTNAME)
                                                            .addMetadata(AmazonInfo.MetaDataKey.publicIpv4, IP_ADDRESS)
                                                            .addMetadata(AmazonInfo.MetaDataKey.localHostname, HOSTNAME)
                                                            .addMetadata(AmazonInfo.MetaDataKey.localIpv4, IP_ADDRESS)
                                                            .build();
            final InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                                                                  .setInstanceId("EVCACHE-1")
                                                                  .setAppName("TODO")
                                                                  .setStatus(InstanceInfo.InstanceStatus.UP)
                                                                  .setHostName(HOSTNAME)
                                                                  .setIPAddr(IP_ADDRESS)
                                                                  .setDataCenterInfo(amazonInfo)
                                                                  .setASGName("SHARD1")
                                                                  .add("evcache.port",
                                                                       environment.getProperty("evcache.port"))
                                                                  .build();
            application.addInstance(instanceInfo);
            final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
            doReturn(application).when(discoveryClient).getApplication("TODO");
            return discoveryClient;
        }

        @Bean
        public MemcachedClientFactoryBean memcachedClient(final ConfigurableEnvironment environment) {
            final MemcachedClientFactoryBean bean = new MemcachedClientFactoryBean();
            bean.setServers("localhost:" + environment.getProperty("evcache.port"));
            return bean;
        }

        @Repository
        static class TodoRepository {
            @Cacheable(cacheNames = "todos", key = "'findAll'")
            public List<Todo> findAll() {
                return Arrays.asList(new Todo("first"), new Todo("second"));
            }
        }

        static class Todo implements Serializable {
            private static final long serialVersionUID = -632306557208393893L;
            private String title;

            Todo(final String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }

            @Override
            public boolean equals(final Object that) {
                if (this == that) {
                    return true;
                }
                if (that == null || getClass() != that.getClass()) {
                    return false;
                }
                final Todo todo = (Todo) that;
                return Objects.equals(title, todo.title);
            }

            @Override
            public int hashCode() {
                return Objects.hash(title);
            }
        }
    }
}
