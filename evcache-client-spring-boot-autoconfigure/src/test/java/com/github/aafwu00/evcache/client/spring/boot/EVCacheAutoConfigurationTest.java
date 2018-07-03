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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.evcache.client.spring.EVCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheAutoConfigurationTest {
    private static CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer = mock(CacheManagerCustomizer.class);
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EVCacheAutoConfiguration.class,
                                                     CacheAutoConfiguration.class,
                                                     ArchaiusAutoConfiguration.class));
    }

    @Test
    void should_be_loaded_EVCacheManager() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test", "evcache.clusters[0].cachePrefix=test1")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).hasSingleBean(EVCacheManager.class),
                         () -> assertThat(context).hasSingleBean(EVCacheMeterBinderProvider.class),
                         () -> assertThat(context.getEnvironment().getProperty("evcache.use.simple.node.list.provider")).isEqualTo("true"),
                         () -> verify(cacheManagerCustomizer).customize(any(EVCacheManager.class))
                     ));
    }

    @Test
    void should_be_not_loaded_CacheManager_when_no_configuration() {
        contextRunner.withUserConfiguration(NoCacheableConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(CacheManager.class));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cacheManager_already_exists() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test", "evcache.clusters[0].cachePrefix=test1")
                     .withUserConfiguration(ExistsCacheManagerConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).doesNotHaveBean(EVCacheManager.class),
                         () -> assertThat(context).doesNotHaveBean(EVCacheMeterBinderProvider.class)
                     ));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_evcache_enable_is_false() {
        contextRunner.withPropertyValues("evcache.enabled=false")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).hasSingleBean(CacheManager.class),
                         () -> assertThat(context).doesNotHaveBean(EVCacheManager.class)
                     ));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cache_type_is_none() {
        contextRunner.withPropertyValues("spring.cache.type=none", "evcache.enabled=true")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).hasSingleBean(NoOpCacheManager.class),
                         () -> assertThat(context).doesNotHaveBean(EVCacheManager.class)
                     ));
    }

    @Test
    void should_be_thrown_exception_when_evcache_name_is_blank() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_is_empty() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_name_is_blank() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test",
                                         "evcache.clusters[0].cachePrefix=")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_name_contains_colon() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test",
                                         "evcache.clusters[0].cachePrefix=test:123")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_ttl_is_less_then_zero() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test",
                                         "evcache.clusters[0].cachePrefix=test1",
                                         "evcache.clusters[0].timeToLive=-1")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_not_loaded_EVCacheMeterBinder_when_evcache_metrics_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test",
                                         "evcache.clusters[0].cachePrefix=test1",
                                         "evcache.metrics.enabled=false")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(EVCacheMeterBinder.class));
    }

    @Test
    void should_be_loaded_EVCacheProperties() {
        contextRunner.withPropertyValues("evcache.clusters[0].app-name=test",
                                         "evcache.clusters[0].cache-prefix=test1",
                                         "evcache.clusters[0].time-to-live=1000",
                                         "evcache.clusters[0].server-group-retry=true",
                                         "evcache.clusters[0].enable-exception-throwing=true",
                                         "evcache.clusters[0].allow-null-values=false",
                                         "evcache.clusters[1].appName=test",
                                         "evcache.clusters[1].cachePrefix=test2",
                                         "evcache.clusters[1].server-group-retry=false",
                                         "evcache.use.simple.node.list.provider=true")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).hasSingleBean(EVCacheProperties.class),
                         () -> assertThat(firstCluster(context).getAppName()).isEqualTo("test"),
                         () -> assertThat(firstCluster(context).getCachePrefix()).isEqualTo("test1"),
                         () -> assertThat(firstCluster(context).getTimeToLive()).isEqualTo(1000),
                         () -> assertThat(firstCluster(context).isServerGroupRetry()).isTrue(),
                         () -> assertThat(firstCluster(context).isAllowNullValues()).isFalse(),
                         () -> assertThat(secondCluster(context).getAppName()).isEqualTo("test"),
                         () -> assertThat(secondCluster(context).getCachePrefix()).isEqualTo("test2"),
                         () -> assertThat(secondCluster(context).isAllowNullValues()).isTrue(),
                         () -> assertThat(secondCluster(context).isServerGroupRetry()).isFalse()
                     ));
    }

    private EVCacheProperties.Cluster firstCluster(final AssertableApplicationContext context) {
        return property(context).getClusters().get(0);
    }

    private EVCacheProperties property(final AssertableApplicationContext context) {
        return context.getBean(EVCacheProperties.class);
    }

    private EVCacheProperties.Cluster secondCluster(final AssertableApplicationContext context) {
        return property(context).getClusters().get(1);
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
