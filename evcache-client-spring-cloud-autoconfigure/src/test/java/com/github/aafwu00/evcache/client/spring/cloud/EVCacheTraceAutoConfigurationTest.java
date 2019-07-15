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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.evcache.client.spring.boot.EVCacheAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheTraceAutoConfigurationTest {
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class,
                                                     EVCacheTraceAutoConfiguration.class,
                                                     EVCacheAutoConfiguration.class));
    }

    @Test
    void should_be_loaded_EVCacheManagerTraceCustomizer() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test", "evcache.clusters[0].keyPrefix=test1")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(EVCacheManagerTraceCustomizer.class));
    }

    @Test
    void should_be_customized_EVCacheManager() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test", "evcache.clusters[0].keyPrefix=test1")
                     .withUserConfiguration(MockConfiguration.class)
                     .run(context -> verify(context.getBean(EVCacheManagerTraceCustomizer.class)).customize(any()));
    }

    @Test
    void should_be_not_loaded_EVCacheManagerTraceCustomizer_when_not_disabled() {
        contextRunner.withPropertyValues("evcache.clusters[0].appName=test",
                                         "evcache.clusters[0].keyPrefix=test1",
                                         "evcache.trace.enabled=false")
                     .withUserConfiguration(EnableCachingConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(EVCacheManagerTraceCustomizer.class));
    }

    @Configuration
    @EnableCaching
    static class MockConfiguration {
        @Bean
        EVCacheManagerTraceCustomizer evcacheManagerTraceCustomizer() {
            return mock(EVCacheManagerTraceCustomizer.class);
        }
    }

    @Configuration
    @EnableCaching
    static class EnableCachingConfiguration {
    }
}
