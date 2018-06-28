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

package com.github.aafwu00.evcache.client.spring.boot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.github.aafwu00.evcache.client.spring.EVCacheManager;
import com.netflix.evcache.util.EVCacheConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Taeho Kim
 */
class EVCacheAutoConfigurationTest {
    private static CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        cacheManagerCustomizer = mock(CacheManagerCustomizer.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_be_loaded_EVCacheManager() {
        loadContext(EnableCachingConfiguration.class, "evcache.clusters[0].appName=test", "evcache.clusters[0].cachePrefix=test1");
        assertAll(
            () -> assertThat(context.getBean(EVCacheManager.class)).isNotNull(),
            () -> assertThat(context.getBean(EVCacheMetrics.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isTrue(),
            () -> assertThat(EVCacheConfig.getInstance()
                                          .getDynamicBooleanProperty("evcache.use.simple.node.list.provider", false)
                                          .get()).isTrue(),
            () -> verify(cacheManagerCustomizer).customize(any(EVCacheManager.class))
        );
    }

    @Test
    void should_be_not_override_provider_when_already_exists_property() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "evcache.use.simple.node.list.provider=false");
        assertAll(
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isFalse(),
            () -> assertThat(EVCacheConfig.getInstance()
                                          .getDynamicBooleanProperty("evcache.use.simple.node.list.provider", true)
                                          .get()).isFalse()
        );
    }

    @Test
    void should_be_not_loaded_CacheManager_when_has_no_configuration() {
        loadContext(NoCacheableConfiguration.class);
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(CacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cacheManager_already_exists() {
        loadContext(ExistsCacheManagerConfiguration.class, "evcache.clusters[0].appName=test", "evcache.clusters[0].cachePrefix=test1");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_disable_evcache() {
        loadContext(EnableCachingConfiguration.class, "evcache.enabled=false");
        assertAll(
            () -> assertThat(context.getBean(CacheManager.class)).isNotNull(),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cache_type_is_none() {
        loadContext(EnableCachingConfiguration.class, "spring.cache.type=none", "evcache.enabled=true");
        assertAll(
            () -> assertThat(context.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isNull()
        );
    }

    private void assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment(final String... paris) {
        assertThatThrownBy(() -> loadContext(EnableCachingConfiguration.class,
                                             paris)).isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                    .hasCauseExactlyInstanceOf(BeanCreationException.class);
    }

    @Test
    void should_be_thrown_exception_when_evcache_cachePrefix_is_empty() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.clusters[0].appName=test");
    }

    @Test
    void should_be_thrown_exception_when_evcache_cachePrefix_is_blank() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.clusters[0].appName=test",
                                                                       "evcache.clusters[0].cachePrefix=");
    }

    @Test
    void should_be_thrown_exception_when_evcache_cachePrefix_contains_colon() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.clusters[0].appName=test",
                                                                       "evcache.clusters[0].cachePrefix=test:123");
    }

    @Test
    void should_be_thrown_exception_when_evcache_cluster_ttl_is_less_then_zero() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.clusters[0].appName=test",
                                                                       "evcache.clusters[0].cachePrefix=test1",
                                                                       "evcache.clusters[0].timeToLive=-1");
    }

    @Test
    void should_be_not_loaded_EVCacheMetrics_when_disable_evcache_metrics() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "evcache.metrics.enabled=false");
        assertThatThrownBy(() -> context.getBean(EVCacheMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_loaded_EVCacheProperties() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.clusters[0].appName=test",
                    "evcache.clusters[0].cachePrefix=test1",
                    "evcache.clusters[0].time-to-live=1000",
                    "evcache.clusters[0].server-group-retry=true",
                    "evcache.clusters[0].enable-exception-throwing=true",
                    "evcache.clusters[0].allow-null-values=false",
                    "evcache.clusters[1].appName=test",
                    "evcache.clusters[1].cachePrefix=test2",
                    "evcache.clusters[1].server-group-retry=false");
        final EVCacheProperties properties = context.getBean(EVCacheProperties.class);
        assertAll(
            () -> assertThat(first(properties).getAppName()).isEqualTo("test"),
            () -> assertThat(first(properties).getCachePrefix()).isEqualTo("test1"),
            () -> assertThat(first(properties).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(first(properties).isServerGroupRetry()).isTrue(),
            () -> assertThat(first(properties).isAllowNullValues()).isFalse(),
            () -> assertThat(second(properties).getAppName()).isEqualTo("test"),
            () -> assertThat(second(properties).getCachePrefix()).isEqualTo("test2"),
            () -> assertThat(second(properties).isAllowNullValues()).isTrue(),
            () -> assertThat(second(properties).isServerGroupRetry()).isFalse()
        );
    }

    private EVCacheProperties.Cluster first(final EVCacheProperties properties) {
        return properties.getClusters().get(0);
    }

    private EVCacheProperties.Cluster second(final EVCacheProperties properties) {
        return properties.getClusters().get(1);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(EVCacheAutoConfiguration.class);
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
        CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer() {
            return cacheManagerCustomizer;
        }
    }

    @Configuration
    @EnableCaching
    static class ExistsCacheManagerConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new SimpleCacheManager();
        }
    }
}
