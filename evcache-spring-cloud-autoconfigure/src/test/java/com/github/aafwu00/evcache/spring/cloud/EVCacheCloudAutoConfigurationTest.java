/*
 * Copyright 2017-2018 the original author or authors.
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

package com.github.aafwu00.evcache.spring.cloud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.connection.ConnectionFactoryBuilder;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.SimpleNodeListProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Taeho Kim
 */
class EVCacheCloudAutoConfigurationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_be_loaded_EVCacheClientPoolManager() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(ConnectionFactoryBuilder.class)).isNotNull(),
            () -> assertThat(context.getBean(DataCenterAwareEurekaNodeListProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isFalse()
        );
    }

    @Test
    void should_not_be_loaded_EVCacheClientPoolManager_when_evcache_cloud_disabled() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "evcache.cloud.enabled=false",
                    "spring.application.name=test");
        assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_loaded_EVCacheClientPoolManager_when_not_exists_eurekaClient() {
        loadContext(NoEurekaClientConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(ConnectionFactoryBuilder.class)).isNotNull(),
            () -> assertThat(context.getBean(SimpleNodeListProvider.class)).isNotNull()
        );
    }

    @Test
    void should_be_loaded_EVCacheClientPoolManager_when_exists_evcacheNodeList() {
        loadContext(ExistsEVCacheNodeListConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(ConnectionFactoryBuilder.class)).isNotNull(),
            () -> assertThat(context.getBean(EVCacheNodeList.class)).isNotNull()
                                                                    .isNotInstanceOfAny(SimpleNodeListProvider.class,
                                                                                        DataCenterAwareEurekaNodeListProvider.class)
        );
    }

    @Test
    void should_be_not_loaded_EVCacheClientPoolManager_when_not_exists_not_evcacheManager() {
        loadContext(EnableCachingConfiguration.class, "spring.cache.type=none", "spring.application.name=test");
        assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_EVCacheClientPoolManager_when_not_enableCaching() {
        loadContext(NoCacheableConfiguration.class, "spring.cache.type=none", "spring.application.name=test");
        assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(EVCacheCloudAutoConfiguration.class);
        context.register(CacheAutoConfiguration.class);
        context.refresh();
    }

    @Configuration
    static class NoCacheableConfiguration {
    }

    @Configuration
    @EnableCaching
    static class EnableCachingConfiguration {
        @Bean
        ApplicationInfoManager applicationInfoManager() {
            final ApplicationInfoManager result = mock(ApplicationInfoManager.class);
            final InstanceInfo instanceInfo = mock(InstanceInfo.class);
            doReturn(instanceInfo).when(result).getInfo();
            final DataCenterInfo dataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.MyOwn);
            doReturn(dataCenterInfo).when(instanceInfo).getDataCenterInfo();
            return result;
        }

        @Bean
        EurekaClient eurekaClient() {
            return mock(EurekaClient.class);
        }
    }

    @Configuration
    @EnableCaching
    static class ExistsEVCacheNodeListConfiguration {
        @Bean
        EVCacheNodeList evcacheNodeList() {
            return mock(EVCacheNodeList.class);
        }
    }

    @Configuration
    @EnableCaching
    static class NoEurekaClientConfiguration {
    }
}
