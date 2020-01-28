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

import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;
import com.couchbase.mock.memcached.Item;
import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.couchbase.mock.memcached.Storage.StorageType.CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
@Tag("integration")
class EVCacheSpringCloudIntegrationTest {
    private static CouchbaseMock server;
    private static ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
    private static DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    @Autowired
    private TodoApp.TodoRepository repository;

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        final BucketConfiguration config = new BucketConfiguration();
        config.bucketStartPort = 11211;
        config.numNodes = 1;
        config.type = Bucket.BucketType.MEMCACHED;
        config.name = "memcached";
        server = new CouchbaseMock(11210, Collections.singletonList(config));
        server.start();
        server.waitForStartup();
        final String hostname = "localhost";
        final String ipAddress = "127.0.0.1";
        doReturn(InstanceInfo.Builder.newBuilder()
                                     .setAppName("test")
                                     .setStatus(InstanceInfo.InstanceStatus.UP)
                                     .setHostName(hostname)
                                     .setIPAddr(ipAddress)
                                     .build())
            .when(applicationInfoManager).getInfo();
        final Application application = new Application("TODO");
        final AmazonInfo amazonInfo = AmazonInfo.Builder.newBuilder()
                                                        .addMetadata(AmazonInfo.MetaDataKey.availabilityZone, "DEFAULT")
                                                        .addMetadata(AmazonInfo.MetaDataKey.publicHostname, hostname)
                                                        .addMetadata(AmazonInfo.MetaDataKey.publicIpv4, ipAddress)
                                                        .addMetadata(AmazonInfo.MetaDataKey.localHostname, hostname)
                                                        .addMetadata(AmazonInfo.MetaDataKey.localIpv4, ipAddress)
                                                        .build();
        final InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                                                              .setInstanceId("EVCACHE-1")
                                                              .setAppName("TODO")
                                                              .setStatus(InstanceInfo.InstanceStatus.UP)
                                                              .setHostName(hostname)
                                                              .setIPAddr(ipAddress)
                                                              .setDataCenterInfo(amazonInfo)
                                                              .setASGName("SHARD1")
                                                              .add("evcache.port", "11211")
                                                              .build();
        application.addInstance(instanceInfo);
        doReturn(application).when(discoveryClient).getApplication("TODO");
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void cached() {
        repository.findAll();
        final Iterable<Item> items = server.getBuckets().get("memcached").getMasterItems(CACHE);
        assertThat(items).isNotEmpty()
                         .allMatch(item -> "todos:findAll".equals(item.getKeySpec().key));
    }

    @SpringBootApplication
    @EnableCaching
    @EnableDiscoveryClient
    @EnableAutoConfiguration(exclude = GsonAutoConfiguration.class)
    static class TodoApp {
        @Bean
        public ApplicationInfoManager applicationInfoManager() {
            return applicationInfoManager;
        }

        @Bean
        public DiscoveryClient eurekaClient() {
            return discoveryClient;
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
        }
    }
}
