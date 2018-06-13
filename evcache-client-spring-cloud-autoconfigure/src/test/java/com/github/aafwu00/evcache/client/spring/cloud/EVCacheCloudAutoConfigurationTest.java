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

package com.github.aafwu00.evcache.client.spring.cloud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.connection.IConnectionFactoryProvider;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import com.netflix.evcache.util.EVCacheConfig;

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
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(IConnectionFactoryProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isFalse(),
            () -> assertThat(EVCacheConfig.getInstance()
                                          .getDynamicBooleanProperty("evcache.use.simple.node.list.provider", true)
                                          .get()).isFalse()
        );
    }

    @Test
    void should_be_loaded_EVCacheClientPoolManager_when_simpleNodeListProvider_is_false() {
        loadContext(EnableCachingConfigurationWithProxyBean.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "spring.application.name=test",
                    "evcache.use.simple.node.list.provider=false");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(IConnectionFactoryProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isFalse(),
            () -> assertThat(EVCacheConfig.getInstance()
                                          .getDynamicBooleanProperty("evcache.use.simple.node.list.provider", true)
                                          .get()).isFalse()
        );
    }

    @Test
    void should_be_loaded_EVCacheClientPoolManager_when_exists_EVCacheClientPoolManager() {
        loadContext(ExistsEVCacheClientPoolManagerConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThat(context.getBean(EVCacheClientPoolManager.class)).isNotNull(),
            () -> assertThat(context.getBean(IConnectionFactoryProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_not_be_loaded_EVCacheClientPoolManager_when_disabled_evcache_cloud() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "evcache.cloud.enabled=false",
                    "spring.application.name=test");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(
                NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_not_be_loaded_EVCacheClientPoolManager_when_simpleNodeListProvider_is_true() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "evcache.cloud.enabled=false",
                    "evcache.use.simple.node.list.provider=true");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(
                NoSuchBeanDefinitionException.class)
        );
    }

    @Test
    void should_not_be_loaded_EVCacheClientPoolManager_when_not_exists_eurekaClient() {
        loadContext(NoEurekaClientConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "spring.application.name=test");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(
                NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_be_not_loaded_EVCacheClientPoolManager_when_not_exists_not_evcacheManager() {
        loadContext(EnableCachingConfiguration.class, "spring.cache.type=none", "spring.application.name=test");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(
                NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_be_not_loaded_EVCacheClientPoolManager_when_cache_type_is_none() {
        loadContext(NoCacheableConfiguration.class, "spring.cache.type=none", "spring.application.name=test");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheClientPoolManager.class)).isExactlyInstanceOf(
                NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(EVCacheCloudAutoConfiguration.class);
        context.register(CacheAutoConfiguration.class);
        context.register(ArchaiusAutoConfiguration.class);
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
            return mock(DiscoveryClient.class);
        }
    }

    @Configuration
    @EnableCaching
    static class EnableCachingConfigurationWithProxyBean extends EnableCachingConfiguration {
        @Bean
        ApplicationInfoManager applicationInfoManager() {
            return super.applicationInfoManager();
        }

        @Bean
        EurekaClient eurekaClient() {
            return (EurekaClient) new DefaultAopProxyFactory().createAopProxy(new ProxyFactory(mock(DiscoveryClient.class))).getProxy();
        }
    }

    @Configuration
    @EnableCaching
    static class ExistsEVCacheClientPoolManagerConfiguration extends EnableCachingConfiguration {
        @Bean
        EVCacheClientPoolManager evCacheClientPoolManager() {
            return mock(EVCacheClientPoolManager.class);
        }
    }

    @Configuration
    @EnableCaching
    static class NoEurekaClientConfiguration {
    }
}
