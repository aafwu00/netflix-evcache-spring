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
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.micrometer.MicrometerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.util.CollectionUtils.findValueOfType;

/**
 * @author Taeho Kim
 */
class EVCacheMetricsAutoConfigurationTest {
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EVCacheMetricsAutoConfiguration.class));
    }

    @AfterEach
    void tearDown() {
        setField(Spectator.globalRegistry(), "registries", new ArrayList<>());
    }

    @Test
    void should_be_loaded_EVCacheMeterBinderProvider() {
        contextRunner.withUserConfiguration(EnableEVCacheManagerConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context).hasSingleBean(EVCacheMeterBinderProvider.class),
                         () -> assertThat(context).hasSingleBean(EVCacheMetricsAutoConfiguration.SpectatorRegistration.class)
                     ));
    }

    @Test
    void should_be_not_loaded_EVCacheMeterBinderProvider_when_evcache_metrics_enable_is_false() {
        contextRunner.withPropertyValues("evcache.metrics.enabled=false")
                     .withUserConfiguration(EnableEVCacheManagerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(EVCacheMeterBinderProvider.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_be_not_loaded_MicrometerRegistry_when_MeterRegistry_not_exists() {
        contextRunner.withUserConfiguration(EnableEVCacheManagerConfiguration.class)
                     .run(context -> {
                         final List<Registry> registries = (List<Registry>) getField(Spectator.globalRegistry(), "registries");
                         assertThat(registries).isEmpty();
                     });
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_be_loaded_MicrometerRegistry_when_MeterRegistry() {
        contextRunner.withUserConfiguration(EnableMicrometerRegistryConfiguration.class)
                     .run(context -> {
                         final List<Registry> registries = (List<Registry>) getField(Spectator.globalRegistry(), "registries");
                         assertThat(findValueOfType(registries, MicrometerRegistry.class)).isNotNull();
                     });
    }

    @Configuration
    static class NoEVCacheManagerConfiguration {
    }

    @Configuration
    static class EnableEVCacheManagerConfiguration {
        @Bean
        EVCacheManager cacheManager() {
            return mock(EVCacheManager.class);
        }
    }

    @Configuration
    static class EnableMicrometerRegistryConfiguration {
        @Bean
        EVCacheManager cacheManager() {
            return mock(EVCacheManager.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
