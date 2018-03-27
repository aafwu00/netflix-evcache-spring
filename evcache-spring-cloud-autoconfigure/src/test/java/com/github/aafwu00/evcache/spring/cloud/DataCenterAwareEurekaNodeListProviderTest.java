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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;

import static com.netflix.appinfo.DataCenterInfo.Name.Amazon;
import static com.netflix.appinfo.DataCenterInfo.Name.MyOwn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author Taeho Kim
 */
class DataCenterAwareEurekaNodeListProviderTest {
    private InstanceInfo instanceInfo;
    private DataCenterInfo dataCenterInfo;
    private DataCenterAwareEurekaNodeListProvider provider;

    @BeforeEach
    void setUp() {
        final ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
        final EurekaClient eurekaClient = mock(DiscoveryClient.class);
        instanceInfo = mock(InstanceInfo.class);
        doReturn(instanceInfo).when(applicationInfoManager).getInfo();
        dataCenterInfo = mock(DataCenterInfo.class);
        doReturn(dataCenterInfo).when(instanceInfo).getDataCenterInfo();
        provider = new DataCenterAwareEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }

    @Test
    void should_be_MyOwnEurekaNodeListProvider_when_dataCenter_is_MyOwn() {
        assertThatDelegateIsExactlyInstanceOf(MyOwn, MyOwnEurekaNodeListProvider.class);
    }

    @Test
    void should_be_MyOwnEurekaNodeListProvider_when_dataCenterInfo_is_null() {
        doReturn(null).when(instanceInfo).getDataCenterInfo();
        assertThatDelegateIsExactlyInstanceOf(Amazon, MyOwnEurekaNodeListProvider.class);
    }

    @Test
    void should_be_AmazonEurekaNodeListProvider_when_dataCenter_is_Amazon() {
        assertThatDelegateIsExactlyInstanceOf(Amazon, AmazonEurekaNodeListProvider.class);
    }

    @Test
    void discoverInstances() throws IOException {
        final EVCacheNodeList delegate = mock(EVCacheNodeList.class);
        setField(provider, "delegate", delegate);
        provider.afterPropertiesSet();
        final Map<ServerGroup, EVCacheServerGroupConfig> result = new HashMap<>();
        doReturn(result).when(delegate).discoverInstances("appName");
        assertThat(provider.discoverInstances("appName")).isSameAs(result);
        verify(delegate).discoverInstances("appName");
    }

    private void assertThatDelegateIsExactlyInstanceOf(DataCenterInfo.Name dataCenter, Class<? extends EVCacheNodeList> clazz) {
        doReturn(dataCenter).when(dataCenterInfo).getName();
        provider.afterPropertiesSet();
        assertThat(getField(provider, "delegate")).isExactlyInstanceOf(clazz);
    }
}
