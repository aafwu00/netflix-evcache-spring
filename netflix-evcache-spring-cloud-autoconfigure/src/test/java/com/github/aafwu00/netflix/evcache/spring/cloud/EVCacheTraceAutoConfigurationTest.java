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

package com.github.aafwu00.netflix.evcache.spring.cloud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.NoOpSpanLogger;
import org.springframework.cloud.sleuth.log.SpanLogger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import com.github.aafwu00.netflix.evcache.spring.boot.EVCacheAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Taeho Kim
 */
class EVCacheTraceAutoConfigurationTest {
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
    void should_be_loaded_EVCacheManagerTraceCustomizer() {
        loadContext(EnableCachingConfiguration.class, "evcache.name=test", "evcache.prefixes[0].name=test1");
        assertThat(context.getBean(EVCacheManagerTraceCustomizer.class)).isNotNull();
    }

    @Test
    void should_be_customized_EVCacheManager() {
        loadContext(MockConfiguration.class, "evcache.name=test", "evcache.prefixes[0].name=test1");
        verify(context.getBean(EVCacheManagerTraceCustomizer.class)).customize(any());
    }

    @Test
    void should_be_not_loaded_EVCacheManagerTraceCustomizer_when_not_disabled() {
        loadContext(EnableCachingConfiguration.class,
                    "evcache.name=test", "evcache.prefixes[0].name=test1", "evcache.trace.enabled=false");
        assertThatThrownBy(() -> context.getBean(EVCacheManagerTraceCustomizer.class))
            .isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(TraceAutoConfiguration.class);
        context.register(EVCacheTraceAutoConfiguration.class);
        context.register(EVCacheAutoConfiguration.class);
        context.refresh();
    }

    @Configuration
    @EnableCaching
    static class MockConfiguration {
        @Bean
        EVCacheManagerTraceCustomizer evcacheManagerTraceCustomizer() {
            return mock(EVCacheManagerTraceCustomizer.class);
        }

        @Bean
        ConversionService conversionService() {
            return new DefaultConversionService();
        }

        @Bean
        SpanLogger spanLogger() {
            return new NoOpSpanLogger();
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
        SpanLogger spanLogger() {
            return new NoOpSpanLogger();
        }
    }
}
