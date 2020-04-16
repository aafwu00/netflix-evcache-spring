/*
 * Copyright 2017-2020 the original author or authors.
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

package com.github.aafwu00.evcache.server.spring.cloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
class MemcachedReactiveHealthContributorAutoConfigurationTest {
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class,
                                                     EurekaDiscoveryClientConfiguration.class,
                                                     EurekaClientAutoConfiguration.class,
                                                     EVCacheServerAutoConfiguration.class,
                                                     MemcachedAutoConfiguration.class,
                                                     MemcachedReactiveHealthContributorAutoConfiguration.class))
            .withPropertyValues("spring.cloud.service-registry.auto-registration.enabled=false");
    }

    @Test
    void should_be_loaded_MemcachedReactiveHealthIndicator_when_exists_EnableEVCacheServer() {
        contextRunner.withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(MemcachedReactiveHealthIndicator.class));
    }

    @Test
    void should_be_not_loaded_MemcachedReactiveHealthIndicator_when_not_exists_EnableEVCacheServer() {
        contextRunner.withUserConfiguration(NoEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedReactiveHealthIndicator.class));
    }

    @Test
    void should_be_loaded_MemcachedReactiveHealthIndicator_when_management_health_defaults_enabled_is_true() {
        contextRunner.withPropertyValues("management.health.defaults.enabled=true")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(MemcachedReactiveHealthIndicator.class));
    }

    @Test
    void should_be_not_loaded_MemcachedReactiveHealthIndicator_when_management_health_defaults_enabled_is_false() {
        contextRunner.withPropertyValues("management.health.defaults.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedReactiveHealthIndicator.class));
    }

    @Test
    void should_be_loaded_MemcachedReactiveHealthIndicator_when_management_health_memcached_enabled_is_true() {
        contextRunner.withPropertyValues("management.health.defaults.enabled=false",
                                         "management.health.memcached.enabled=true")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).hasSingleBean(MemcachedReactiveHealthIndicator.class));
    }

    @Test
    void should_be_not_loaded_MemcachedReactiveHealthIndicator_when_management_health_memcached_enabled_is_false() {
        contextRunner.withPropertyValues("management.health.defaults.enabled=true",
                                         "management.health.memcached.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedReactiveHealthIndicator.class));
    }

    @Configuration
    static class NoEVCacheServerConfiguration {
    }

    @Configuration
    @EnableEVCacheServer
    static class EnableEVCacheServerConfiguration {
    }
}
