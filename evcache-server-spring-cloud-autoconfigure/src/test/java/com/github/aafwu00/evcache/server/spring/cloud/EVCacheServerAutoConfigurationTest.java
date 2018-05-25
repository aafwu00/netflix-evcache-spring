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

package com.github.aafwu00.evcache.server.spring.cloud;

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

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;

import net.spy.memcached.MemcachedClient;

import static com.netflix.appinfo.AmazonInfo.MetaDataKey.availabilityZone;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicHostname;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicIpv4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Taeho Kim
 */
class EVCacheServerAutoConfigurationTest {
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
        loadContext(EnableEVCacheServerConfiguration.class);
        final EurekaInstanceConfigBean config = context.getBean(EurekaInstanceConfigBean.class);
        final AmazonInfo amazonInfo = getAmazonInfo(config);
        assertAll(
            () -> assertThat(context.getBean(ManagementMetadataProvider.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedClient.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedHealthCheckHandler.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedMetrics.class)).isNotNull(),
            () -> assertThat(context.getBean(MemcachedHealthIndicator.class)).isNotNull(),
            () -> assertThat(config.getASGName()).isEqualTo("DEFAULT"),
            () -> assertThat(amazonInfo.get(availabilityZone)).isEqualTo("defaultZone"),
            () -> assertThat(amazonInfo.get(publicHostname)).isNotBlank(),
            () -> assertThat(amazonInfo.get(publicIpv4)).isNotBlank()
        );
    }

    private AmazonInfo getAmazonInfo(final EurekaInstanceConfigBean config) {
        final DataCenterInfo dataCenter = config.getDataCenterInfo();
        if (dataCenter instanceof AmazonInfo) {
            return (AmazonInfo) dataCenter;
        }
        return null;
    }

    @Test
    void should_be_overloaded_properties_when_EurekaInstanceConfigBean_is_loaded() {
        loadContext(EnableEVCacheServerConfiguration.class,
                    "evcache.port=1",
                    "evcache.secure.port=2",
                    "rend.port=3",
                    "rend.batch.port=4",
                    "udsproxy.memcached.port=5",
                    "udsproxy.memento.port=6",
                    "evcache.asg-name=test1",
                    "evcache.availability-zone=test2",
                    "evcache.public-hostname=test3",
                    "evcache.public-ipv4=test4",
                    "evcache.server.health.enabled=false");
        final EurekaInstanceConfigBean config = context.getBean(EurekaInstanceConfigBean.class);
        final AmazonInfo amazonInfo = getAmazonInfo(config);
        assertAll(
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.port", "1"),
            () -> assertThat(config.getMetadataMap()).containsEntry("evcache.secure.port", "2"),
            () -> assertThat(config.getMetadataMap()).containsEntry("rend.port", "3"),
            () -> assertThat(config.getMetadataMap()).containsEntry("rend.batch.port", "4"),
            () -> assertThat(config.getMetadataMap()).containsEntry("udsproxy.memcached.port", "5"),
            () -> assertThat(config.getMetadataMap()).containsEntry("udsproxy.memento.port", "6"),
            () -> assertThat(config.getASGName()).isEqualTo("test1"),
            () -> assertThat(amazonInfo.get(availabilityZone)).isEqualTo("test2"),
            () -> assertThat(amazonInfo.get(publicHostname)).isEqualTo("test3"),
            () -> assertThat(amazonInfo.get(publicIpv4)).isEqualTo("test4")
        );
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_EVCacheServer_health_enabled_is_false() {
        loadContext(EnableEVCacheServerConfiguration.class, "evcache.server.health.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedClient.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedHealthCheckHandler_when_evcache_health_eureka_enabled_is_false() {
        loadContext(EnableEVCacheServerConfiguration.class, "evcache.server.health.eureka.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedHealthCheckHandler.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException
                                                                                                             .class);
    }

    @Test
    void should_be_not_loaded_MemcachedMetrics_when_evcache_metrics_enabled_is_false() {
        loadContext(EnableEVCacheServerConfiguration.class, "evcache.server.metrics.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedMetrics.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedHealthIndicator_when_evcache_health_memcached_enabled_is_false() {
        loadContext(EnableEVCacheServerConfiguration.class, "evcache.server.health.memcached.enabled=false");
        assertThatThrownBy(() -> context.getBean(MemcachedHealthIndicator.class)).isExactlyInstanceOf(
            NoSuchBeanDefinitionException.class);
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_not_exists_EnableEVCacheServer() {
        loadContext(NoEVCacheServerConfiguration.class);
        assertThatThrownBy(() -> context.getBean(MemcachedClient.class)).isExactlyInstanceOf(NoSuchBeanDefinitionException.class);
    }

    private void loadContext(final Class<?> configuration, final String... pairs) {
        addEnvironment(context, pairs);
        context.register(configuration);
        context.register(UtilAutoConfiguration.class);
        context.register(EurekaDiscoveryClientConfiguration.class);
        context.register(EurekaClientAutoConfiguration.class);
        context.register(EurekaDiscoveryClientConfigServiceAutoConfiguration.class);
        context.register(EVCacheServerAutoConfiguration.class);
        context.register(EVCacheServerHealthAutoConfiguration.class);
        context.refresh();
    }

    @Configuration
    static class NoEVCacheServerConfiguration {
    }

    @Configuration
    @EnableEVCacheServer
    static class EnableEVCacheServerConfiguration {
    }
}
