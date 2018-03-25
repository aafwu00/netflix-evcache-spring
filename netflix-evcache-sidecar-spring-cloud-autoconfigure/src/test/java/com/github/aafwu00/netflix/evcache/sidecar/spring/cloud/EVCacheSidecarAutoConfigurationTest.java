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

package com.github.aafwu00.netflix.evcache.sidecar.spring.cloud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.config.EurekaDiscoveryClientConfigServiceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import net.spy.memcached.MemcachedClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Taeho Kim
 */
class EVCacheSidecarAutoConfigurationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SLF4JLogger");
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_be_loaded_EnableEVCacheServerConfiguration() {
        loadContext(EnableEVCacheSidecarConfiguration.class);
        final EurekaInstanceConfigBean config = context.getBean(EurekaInstanceConfigBean.class);
        assertAll(
            () -> assertThat(context.getBean(ManagementMetadataProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedClient.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedHealthCheckHandler.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedMetrics.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedHealthIndicator.class)).isNotNull(),
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.port", "11211"),
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.group", "Default"),
            () -> assertThat(config.getMetadataMap()).doesNotContainKeys("rend.port"),
            () -> assertThat(config.getMetadataMap()).doesNotContainKeys("rend.batch.port"),
            () -> assertThat(config.getMetadataMap()).doesNotContainKeys("udsproxy.memcached.port"),
            () -> assertThat(config.getMetadataMap()).doesNotContainKeys("udsproxy.memento.port")
        );
    }

    @Test
    void should_be_overloaded_properties_when_EurekaInstanceConfigBean_is_loaded() {
        loadContext(EnableEVCacheSidecarConfiguration.class,
                    "evcache.port=1",
                    "evcache.group=Default1",
                    "rend.port=2",
                    "rend.batch.port=3",
                    "udsproxy.memcached.port=4",
                    "udsproxy.memento.port=5");
        final EurekaInstanceConfigBean config = context.getBean(EurekaInstanceConfigBean.class);
        assertAll(
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.port", "1"),
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.group", "Default1"),
            () -> assertThat(config.getMetadataMap()).containsEntry("rend.port", "2"),
            () -> assertThat(config.getMetadataMap()).containsEntry("rend.batch.port", "3"),
            () -> assertThat(config.getMetadataMap()).containsEntry("udsproxy.memcached.port", "4"),
            () -> assertThat(config.getMetadataMap()).containsEntry("udsproxy.memento.port", "5")
        );
    }

    @Test
    void should_be_not_loaded_EnableEVCacheServerConfiguration_when_evcache_sidecar_enabled_is_false() {
        loadContext(EnableEVCacheSidecarConfiguration.class, "evcache.sidecar.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedClient.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedHealthCheckHandler_when_evcache_sidecar_health_eureka_enabled_is_false() {
        loadContext(EnableEVCacheSidecarConfiguration.class, "evcache.sidecar.health.eureka.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedHealthCheckHandler.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException
                                                                                                             .class);
    }

    @Test
    void should_be_not_loaded_MemcachedMetrics_when_evcache_sidecar_metrics_enabled_is_false() {
        loadContext(EnableEVCacheSidecarConfiguration.class, "evcache.sidecar.metrics.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedHealthIndicator_when_evcache_sidecar_health_memcached_enabled_is_false() {
        loadContext(EnableEVCacheSidecarConfiguration.class, "evcache.sidecar.health.memcached.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedHealthIndicator.class)).isExactlyInstanceOf(
            NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_not_exists_EnableEVCacheServer() {
        loadContext(NoEVCacheSidecarConfiguration.class);
        assertThatThrownBy(() -> context.getBean(MemcachedClient.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(UtilAutoConfiguration.class);
        context.register(EurekaDiscoveryClientConfiguration.class);
        context.register(EurekaClientAutoConfiguration.class);
        context.register(EurekaDiscoveryClientConfigServiceAutoConfiguration.class);
        context.refresh();
    }

    @Configuration
    static class NoEVCacheSidecarConfiguration {
    }

    @Configuration
    @EnableEVCacheSidecar
    static class EnableEVCacheSidecarConfiguration {
    }
}
