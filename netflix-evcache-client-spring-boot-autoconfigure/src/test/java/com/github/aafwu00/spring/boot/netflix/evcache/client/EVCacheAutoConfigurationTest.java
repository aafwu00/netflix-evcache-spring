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

package com.github.aafwu00.spring.boot.netflix.evcache.client;

import java.util.Arrays;

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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;

import com.github.aafwu00.spring.netflix.evcache.client.EVCacheManager;

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
    private static CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer = mock(CacheManagerCustomizer.class);
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
    void should_be_not_loaded_CacheManager_when_no_configuration() {
        loadContext(NoCacheableConfiguration.class);
        assertThatThrownBy(() -> context.getBean(CacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_loaded_EVCacheManager() {
        loadContext(EnableCachingConfiguration.class, "evcache.name=test", "evcache.prefixes[0].name=test1");
        assertAll(
            () -> assertThat(context.getBean(EVCacheManager.class)).isNotNull(),
            () -> assertThat(context.getBean(EVCacheMetrics.class)).isNotNull(),
            () -> assertThat(context.getBean(Environment.class)
                                    .getProperty("evcache.use.simple.node.list.provider", Boolean.class)).isTrue(),
            () -> verify(cacheManagerCustomizer).customize(any(EVCacheManager.class))
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cacheManager_already_exists() {
        loadContext(ExistsCacheManagerConfiguration.class, "evcache.name=test", "evcache.prefixes[0].name=test1");
        assertAll(
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class)
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_evcache_enable_is_false() {
        loadContext(EnableCachingConfiguration.class, "evcache.enabled=false");
        assertAll(
            () -> assertThat(context.getBean(CacheManager.class)).isNotNull(),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class)
        );
    }

    @Test
    void should_be_not_loaded_EVCacheManager_when_cache_type_is_none() {
        loadContext(EnableCachingConfiguration.class, "spring.cache.type=none", "evcache.enabled=true");
        assertAll(
            () -> assertThat(context.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class),
            () -> assertThatThrownBy(() -> context.getBean(EVCacheManager.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class)
        );
    }

    @Test
    void should_be_thrown_exception_when_evcache_name_is_blank() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.name=");
    }

    private void assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment(final String... paris) {
        assertThatThrownBy(() -> loadContext(EnableCachingConfiguration.class,
                                             paris)).isExactlyInstanceOf(UnsatisfiedDependencyException.class)
                                                    .hasCauseExactlyInstanceOf(BeanCreationException.class);
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_is_empty() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.name=test");
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_name_is_blank() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.name=test",
                                                                       "evcache.prefixes[0].name=");
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_name_contains_colon() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.name=test",
                                                                       "evcache.prefixes[0].name=test:123");
    }

    @Test
    void should_be_thrown_exception_when_evcache_prefixes_ttl_is_less_then_zero() {
        assertThatThrownExceptionWhenContextLoadWithInvalidEnvironment("evcache.name=test",
                                                                       "evcache.prefixes[0].name=test1",
                                                                       "evcache.prefixes[0].timeToLive=-1");
    }

    @Test
    void should_be_not_loaded_EVCacheMetrics_when_evcache_metrics_enabled_is_false() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "evcache.metrics.enabled=false");
        assertThatThrownBy(() -> context.getBean(EVCacheMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_loaded_EVCacheProperties() {
        final EVCacheProperties result = new EVCacheProperties();
        result.setEnabled(true);
        result.setName("test");
        final EVCacheProperties.Prefix prefix1 = new EVCacheProperties.Prefix();
        prefix1.setName("test1");
        prefix1.setTimeToLive(1000);
        prefix1.setServerGroupRetry(true);
        prefix1.setEnableExceptionThrowing(true);
        final EVCacheProperties.Prefix prefix2 = new EVCacheProperties.Prefix();
        prefix2.setName("test2");
        prefix2.setServerGroupRetry(false);
        result.setPrefixes(Arrays.asList(prefix1, prefix2));
        loadContext(EnableCachingConfiguration.class,
                    "evcache.name=test",
                    "evcache.prefixes[0].name=test1",
                    "evcache.prefixes[0].time-to-live=1000",
                    "evcache.prefixes[0].server-group-retry=true",
                    "evcache.prefixes[0].enable-exception-throwing=true",
                    "evcache.prefixes[1].name=test2",
                    "evcache.prefixes[1].server-group-retry=false");
        assertThat(context.getBean(EVCacheProperties.class)).isEqualTo(result);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(EVCacheAutoConfiguration.class);
        context.register(CacheAutoConfiguration.class);
        context.refresh();
    }

    @Configuration
    static class NoCacheableConfiguration {
        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }
    }

    @Configuration
    @EnableCaching
    static class EnableCachingConfiguration {
        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }

        @Bean
        CacheManagerCustomizer<EVCacheManager> cacheManagerCustomizer() {
            return cacheManagerCustomizer;
        }
    }

    @Configuration
    @EnableCaching
    static class ExistsCacheManagerConfiguration {
        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }

        @Bean
        CacheManager cacheManager() {
            return new SimpleCacheManager();
        }
    }
}
