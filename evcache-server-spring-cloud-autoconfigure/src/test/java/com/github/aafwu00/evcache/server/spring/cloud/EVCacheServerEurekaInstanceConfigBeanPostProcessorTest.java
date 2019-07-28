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
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.netflix.appinfo.AmazonInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheServerEurekaInstanceConfigBeanPostProcessorTest {
    private ConfigurableEnvironment environment;
    private EurekaInstanceConfigBean eurekaInstanceConfigBean;
    private EurekaClientConfigBean eurekaClientConfigBean;
    private EVCacheServerEurekaInstanceConfigBeanPostProcessor processor;
    private InetUtils.HostInfo hostInfo;

    @BeforeEach
    void setUp() {
        environment = new StandardEnvironment();
        hostInfo = new InetUtils.HostInfo("hostname");
        hostInfo.setIpAddress("ip-address");
        final InetUtils inetUtil = mock(InetUtils.class);
        doReturn(hostInfo).when(inetUtil).findFirstNonLoopbackHostInfo();
        eurekaInstanceConfigBean = spy(new EurekaInstanceConfigBean(inetUtil));
        eurekaClientConfigBean = spy(new EurekaClientConfigBean());
        processor = new EVCacheServerEurekaInstanceConfigBeanPostProcessor(environment, eurekaClientConfigBean);
    }

    @Test
    void should_be_do_nothing_when_before_initialization() {
        assertThat(processor.postProcessBeforeInitialization(eurekaInstanceConfigBean, "beanName")).isEqualTo(eurekaInstanceConfigBean);
    }

    @Test
    void should_be_do_nothing_when_not_EurekaInstanceConfigBean() {
        assertThat(processor.postProcessAfterInitialization(eurekaClientConfigBean, "beanName")).isEqualTo(eurekaClientConfigBean);
        verify(eurekaInstanceConfigBean, never()).setDataCenterInfo(any());
    }

    @Test
    void should_be_do_nothing_when_dataCenter_is_amazon() {
        doReturn(AmazonInfo.Builder.newBuilder().build()).when(eurekaInstanceConfigBean).getDataCenterInfo();
        assertThat(processor.postProcessAfterInitialization(eurekaInstanceConfigBean, "beanName")).isEqualTo(eurekaInstanceConfigBean);
        verify(eurekaInstanceConfigBean, never()).setDataCenterInfo(any());
    }

    @Test
    void should_be_set_metadata_when_exists_property() {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("evcache.availability-zone", "test1");
        metadata.put("evcache.ami-id", "test2");
        metadata.put("evcache.instance-id", "test3");
        metadata.put("evcache.public-hostname", "test4");
        metadata.put("evcache.public-ipv4", "test5");
        metadata.put("evcache.local-ipv4", "test6");
        environment.getPropertySources().addLast(new MapPropertySource("test", metadata));
        assertThat(processor.postProcessAfterInitialization(eurekaInstanceConfigBean, "beanName")).isEqualTo(eurekaInstanceConfigBean);
        final AmazonInfo dataCenterInfo = (AmazonInfo) eurekaInstanceConfigBean.getDataCenterInfo();
        assertThat(dataCenterInfo.getMetadata()).containsEntry("availability-zone", "test1")
                                                .containsEntry("ami-id", "test2")
                                                .containsEntry("instance-id", "test3")
                                                .containsEntry("public-hostname", "test4")
                                                .containsEntry("public-ipv4", "test5")
                                                .containsEntry("local-ipv4", "test6");
    }

    @Test
    void should_be_set_metadata_using_default_when_not_exists_property() {
        eurekaInstanceConfigBean.setInstanceId("test1");
        assertThat(processor.postProcessAfterInitialization(eurekaInstanceConfigBean, "beanName")).isEqualTo(eurekaInstanceConfigBean);
        final AmazonInfo dataCenterInfo = (AmazonInfo) eurekaInstanceConfigBean.getDataCenterInfo();
        assertThat(dataCenterInfo.getMetadata()).containsEntry("availability-zone", "defaultZone")
                                                .containsEntry("ami-id", "n/a")
                                                .containsEntry("instance-id", "test1")
                                                .containsEntry("public-hostname", hostInfo.getHostname())
                                                .containsEntry("public-ipv4", hostInfo.getIpAddress())
                                                .containsEntry("local-ipv4", hostInfo.getIpAddress());
    }

    @Test
    void should_be_set_availabilityZone_using_clientConfig_when_not_exists_property() {
        final String[] zones = {"zone1", "zone2"};
        doReturn("region").when(eurekaClientConfigBean).getRegion();
        doReturn(zones).when(eurekaClientConfigBean).getAvailabilityZones("region");
        assertThat(processor.postProcessAfterInitialization(eurekaInstanceConfigBean, "beanName")).isEqualTo(eurekaInstanceConfigBean);
        final AmazonInfo dataCenterInfo = (AmazonInfo) eurekaInstanceConfigBean.getDataCenterInfo();
        assertThat(dataCenterInfo.getMetadata()).containsEntry("availability-zone", zones[0]);
    }
}
