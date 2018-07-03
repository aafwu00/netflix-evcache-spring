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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.config.EurekaDiscoveryClientConfigServiceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;

import net.spy.memcached.MemcachedClient;

import static com.netflix.appinfo.AmazonInfo.MetaDataKey.availabilityZone;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicHostname;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicIpv4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheServerAutoConfigurationTest {
    private static EurekaInstanceConfigBean eurekaInstanceConfigBean = mock(EurekaInstanceConfigBean.class);
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SLF4JLogger");
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class,
                                                     EurekaDiscoveryClientConfiguration.class,
                                                     EurekaClientAutoConfiguration.class,
                                                     EurekaDiscoveryClientConfigServiceAutoConfiguration.class,
                                                     EVCacheServerAutoConfiguration.class,
                                                     EVCacheServerHealthAutoConfiguration.class
            )).withPropertyValues("spring.cloud.service-registry.auto-registration.enabled=false");
        reset(eurekaInstanceConfigBean);
    }

    @Test
    void should_be_loaded_EnableEVCacheServerConfiguration() {
        contextRunner.withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context.getBean(ManagementMetadataProvider.class)).isNotNull(),
                         () -> assertThat(context.getBean(MemcachedClient.class)).isNotNull(),
                         () -> assertThat(context.getBean(MemcachedHealthCheckHandler.class)).isNotNull(),
                         () -> assertThat(context.getBean(MemcachedHealthIndicator.class)).isNotNull(),
                         () -> assertThat(getConfig(context).getASGName()).isEqualTo("DEFAULT"),
                         () -> assertThat(getAmazonInfo(context).get(availabilityZone)).isEqualTo("defaultZone"),
                         () -> assertThat(getAmazonInfo(context).get(publicHostname)).isNotBlank(),
                         () -> assertThat(getAmazonInfo(context).get(publicIpv4)).isNotBlank()
                     ));
    }

    private EurekaInstanceConfigBean getConfig(AssertableApplicationContext context) {
        return context.getBean(EurekaInstanceConfigBean.class);
    }

    private Map<String, String> getMetadataMap(AssertableApplicationContext context) {
        return getConfig(context).getMetadataMap();
    }

    private AmazonInfo getAmazonInfo(AssertableApplicationContext context) {
        return getAmazonInfo(getConfig(context));
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
        contextRunner.withPropertyValues("evcache.port=1",
                                         "evcache.secure.port=2",
                                         "rend.port=3",
                                         "rend.batch.port=4",
                                         "udsproxy.memcached.port=5",
                                         "udsproxy.memento.port=6",
                                         "evcache.asg-name=test1",
                                         "evcache.availability-zone=test2",
                                         "evcache.public-hostname=test3",
                                         "evcache.public-ipv4=test4",
                                         "evcache.server.health.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(getMetadataMap(context)).containsEntry("evcache.port", "1"),
                         () -> assertThat(getMetadataMap(context)).containsEntry("evcache.secure.port", "2"),
                         () -> assertThat(getMetadataMap(context)).containsEntry("rend.port", "3"),
                         () -> assertThat(getMetadataMap(context)).containsEntry("rend.batch.port", "4"),
                         () -> assertThat(getMetadataMap(context)).containsEntry("udsproxy.memcached.port", "5"),
                         () -> assertThat(getMetadataMap(context)).containsEntry("udsproxy.memento.port", "6"),
                         () -> assertThat(getConfig(context).getASGName()).isEqualTo("test1"),
                         () -> assertThat(getAmazonInfo(context).get(availabilityZone)).isEqualTo("test2"),
                         () -> assertThat(getAmazonInfo(context).get(publicHostname)).isEqualTo("test3"),
                         () -> assertThat(getAmazonInfo(context).get(publicIpv4)).isEqualTo("test4")
                     ));
    }

    @Test
    void should_be_loaded_EnableEVCacheServerConfiguration_with_asg_name() {
        final Map<String, String> metadata = new HashMap<>();
        final DataCenterInfo dataCenterInfo = new MyDataCenterInfo(DataCenterInfo.Name.Amazon);
        doReturn(metadata).when(eurekaInstanceConfigBean).getMetadataMap();
        doReturn("test").when(eurekaInstanceConfigBean).getASGName();
        doReturn(dataCenterInfo).when(eurekaInstanceConfigBean).getDataCenterInfo();
        contextRunner.withPropertyValues("evcache.server.health.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfigurationWithEurekaConfig.class)
                     .run(context -> verify(eurekaInstanceConfigBean).setDataCenterInfo(any(AmazonInfo.class)));
    }

    @Test
    void should_be_loaded_EnableEVCacheServerConfiguration_with_AmazonDataCenter() {
        final Map<String, String> metadata = new HashMap<>();
        final DataCenterInfo dataCenterInfo = new AmazonInfo();
        doReturn(metadata).when(eurekaInstanceConfigBean).getMetadataMap();
        doReturn("test").when(eurekaInstanceConfigBean).getASGName();
        doReturn(dataCenterInfo).when(eurekaInstanceConfigBean).getDataCenterInfo();
        contextRunner.withPropertyValues("evcache.server.health.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfigurationWithEurekaConfig.class)
                     .run(context -> verify(eurekaInstanceConfigBean, never()).setDataCenterInfo(any()));
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_EVCacheServer_health_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedClient.class));
    }

    @Test
    void should_be_not_loaded_MemcachedHealthCheckHandler_when_evcache_health_eureka_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.eureka.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedHealthCheckHandler.class));
    }

    @Test
    void should_be_not_loaded_MemcachedHealthIndicator_when_evcache_health_memcached_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.memcached.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedHealthIndicator.class));
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_not_exists_EnableEVCacheServer() {
        contextRunner.withUserConfiguration(NoEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedClient.class));
    }

    @Configuration
    static class NoEVCacheServerConfiguration {
    }

    @Configuration
    @EnableEVCacheServer
    static class EnableEVCacheServerConfiguration {
    }

    @Configuration
    @EnableEVCacheServer
    static class EnableEVCacheServerConfigurationWithEurekaConfig {
        @Bean
        public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
            return eurekaInstanceConfigBean;
        }

        @Bean
        public EurekaClient eurekaClient() {
            return mock(DiscoveryClient.class);
        }
    }
}
