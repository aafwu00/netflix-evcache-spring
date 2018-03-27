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

package com.github.aafwu00.evcache.spring.cloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.SimpleNodeListProvider;

import static com.github.aafwu00.evcache.spring.cloud.EVCacheCloudProperties.NodeListMode.Amazon;
import static com.github.aafwu00.evcache.spring.cloud.EVCacheCloudProperties.NodeListMode.DataCenterAware;
import static com.github.aafwu00.evcache.spring.cloud.EVCacheCloudProperties.NodeListMode.MyOwn;
import static com.github.aafwu00.evcache.spring.cloud.EVCacheCloudProperties.NodeListMode.Simple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EVCacheCloudPropertiesTest.Config.class, initializers = ConfigFileApplicationContextInitializer.class)
@DirtiesContext
class EVCacheCloudPropertiesTest {
    @Autowired
    private EVCacheCloudProperties properties;
    private ApplicationInfoManager applicationInfoManager;
    private EurekaClient eurekaClient;

    @BeforeEach
    void setUp() {
        applicationInfoManager = mock(ApplicationInfoManager.class);
        eurekaClient = mock(DiscoveryClient.class);
        final InstanceInfo instanceInfo = mock(InstanceInfo.class);
        doReturn(instanceInfo).when(applicationInfoManager).getInfo();
        final DataCenterInfo dataCenterInfo = mock(DataCenterInfo.class);
        doReturn(dataCenterInfo).when(instanceInfo).getDataCenterInfo();
    }

    @Test
    void should_be_load_EVCacheCloudProperties() {
        assertAll(
            () -> assertThat(properties.isEnabled()).isFalse(),
            () -> assertThat(properties.getNodeListMode()).isEqualTo(MyOwn)
        );
    }

    @Test
    void should_be_create_DataCenterAwareEurekaNodeListProvider_when_nodeListMode_is_DataCenterAware() {
        properties.setNodeListMode(DataCenterAware);
        assertThat(properties.createEVCacheNodeList(applicationInfoManager, eurekaClient)).isInstanceOf(
            DataCenterAwareEurekaNodeListProvider.class);
    }

    @Test
    void should_be_create_AmazonEurekaNodeListProvider_when_nodeListMode_is_Amazon() {
        properties.setNodeListMode(Amazon);
        assertThat(properties.createEVCacheNodeList(applicationInfoManager, eurekaClient)).isInstanceOf(AmazonEurekaNodeListProvider.class);
    }

    @Test
    void should_be_create_MyOwnEurekaNodeListProvider_when_nodeListMode_is_MyOwn() {
        properties.setNodeListMode(MyOwn);
        assertThat(properties.createEVCacheNodeList(applicationInfoManager, eurekaClient)).isInstanceOf(MyOwnEurekaNodeListProvider.class);
    }

    @Test
    void should_be_create_SimpleNodeListProvider_when_nodeListMode_is_Simple() {
        properties.setNodeListMode(Simple);
        assertThat(properties.createEVCacheNodeList(applicationInfoManager, eurekaClient)).isInstanceOf(SimpleNodeListProvider.class);
    }

    @Configuration
    @EnableConfigurationProperties(EVCacheCloudProperties.class)
    static class Config {
    }
}
