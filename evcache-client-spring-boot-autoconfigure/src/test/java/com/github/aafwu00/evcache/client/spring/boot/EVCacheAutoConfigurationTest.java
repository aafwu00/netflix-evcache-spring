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

package com.github.aafwu00.evcache.client.spring.boot;

import com.github.aafwu00.evcache.client.spring.EVCacheManager;
import com.github.aafwu00.evcache.client.spring.boot.EVCacheProperties.Cluster;
import com.netflix.evcache.EVCache.Builder;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.function.Supplier;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheAutoConfigurationTest {
    @SuppressWarnings("unchecked")
    private static CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer = mock(CacheManagerCustomizer.class);
    private static Builder.Customizer customizer = mock(Builder.Customizer.class);
    private ApplicationContextRunner contextRunner;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        reset(cacheManagerCustomizer);
        reset(customizer);
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EVCacheAutoConfiguration.class,
                                                     CacheAutoConfiguration.class,
                                                     ArchaiusAutoConfiguration.class));
    }

    @Test
    void should_be_loaded_EVCacheManager() {
        contextRunner.withPropertyValues("evcache.clusters.first.appName=test", "evcache.clusters.first.keyPrefix=test1")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(EVCacheManager.class)
                                                        .hasSingleBean(EVCacheClientPoolManager.class));
        verify(cacheManagerCustomizer).customize(any(EVCacheManager.class));
        verify(customizer).customize(eq("TEST"), any(Builder.class));
    }

    @Test
    void should_be_not_loaded_CacheManager_when_no_configuration() {
        contextRunner.withUserConfiguration(NoCacheableConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(CacheManager.class));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cacheManager_already_exists() {
        contextRunner.withPropertyValues("evcache.clusters.first.appName=test", "evcache.clusters.first.keyPrefix=test1")
                     .withUserConfiguration(ExistsCacheManagerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(EVCacheManager.class)
                                                        .doesNotHaveBean(EVCacheClientPoolManager.class));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_evcache_enable_is_false() {
        contextRunner.withPropertyValues("evcache.enabled=false")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(CacheManager.class)
                                                        .doesNotHaveBean(EVCacheManager.class)
                                                        .doesNotHaveBean(EVCacheClientPoolManager.class));
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cache_type_is_none() {
        contextRunner.withPropertyValues("spring.cache.type=none", "evcache.enabled=true")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(NoOpCacheManager.class)
                                                        .doesNotHaveBean(EVCacheManager.class)
                                                        .doesNotHaveBean(EVCacheClientPoolManager.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_name_is_blank() {
        contextRunner.withPropertyValues("evcache.clusters.first.appName=")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_name_contains_colon() {
        contextRunner.withPropertyValues("evcache.clusters.first.appName=test",
                                         "evcache.clusters.first.keyPrefix=test:123")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                        .hasCauseExactlyInstanceOf(ConfigurationPropertiesBindException.class));
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_ttl_is_less_then_zero() {
        contextRunner.withPropertyValues("evcache.clusters.first.appName=test",
                                         "evcache.clusters.first.keyPrefix=test1",
                                         "evcache.clusters.first.timeToLive=-1s")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasFailed()
                                                        .getFailure()
                                                        .isExactlyInstanceOf(BeanCreationException.class)
                                                        .hasCauseExactlyInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void should_be_loaded_EVCacheProperties() {
        contextRunner.withPropertyValues("evcache.clusters.first.app-name=test",
                                         "evcache.clusters.first.key-prefix=test1",
                                         "evcache.clusters.first.time-to-live=1000s",
                                         "evcache.clusters.first.retry-enabled=true",
                                         "evcache.clusters.first.exception-throwing-enabled=true",
                                         "evcache.clusters.second.appName=test",
                                         "evcache.clusters.second.keyPrefix=test2",
                                         "evcache.clusters.second.retry-enabled=false",
                                         "evcache.use.simple.node.list.provider=true")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(EVCacheProperties.class)
                                                        .getBean(EVCacheProperties.class)
                                                        .matches(this::withFirstCluster)
                                                        .matches(this::withSecondCluster));
    }

    private boolean withFirstCluster(final EVCacheProperties properties) {
        final Cluster first = properties.getClusters().get("first");
        return isEqualTo(first::getAppName, "test") &&
               isEqualTo(first::getKeyPrefix, "test1") &&
               isEqualTo(first::getTimeToLive, ofSeconds(1000)) &&
               isEqualTo(first::isRetryEnabled, true);
    }

    private boolean withSecondCluster(final EVCacheProperties properties) {
        final Cluster second = properties.getClusters().get("second");
        return isEqualTo(second::getAppName, "test") &&
               isEqualTo(second::getKeyPrefix, "test2") &&
               isEqualTo(second::isRetryEnabled, false);
    }

    private <T> boolean isEqualTo(final Supplier<T> actual, final T expect) {
        return Objects.equals(actual.get(), expect);
    }

    @Configuration(proxyBeanMethods = false)
    static class NoCacheableConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class EnableCachingConfiguration {
        @Bean
        CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer() {
            return cacheManagerCustomizer;
        }

        @Bean
        Builder.Customizer customizer() {
            return customizer;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class ExistsCacheManagerConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new SimpleCacheManager();
        }
    }
}
