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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.eureka.EurekaNodeListProvider;

import static com.netflix.appinfo.DataCenterInfo.Name.Amazon;
import static com.netflix.appinfo.DataCenterInfo.Name.MyOwn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Taeho Kim
 */
class DataCenterAwareEurekaNodeListProviderTest {
    private ApplicationInfoManager applicationInfoManager;
    private EurekaClient eurekaClient;
    private DataCenterInfo dataCenterInfo;

    @BeforeEach
    void setUp() {
        applicationInfoManager = mock(ApplicationInfoManager.class);
        eurekaClient = mock(DiscoveryClient.class);
        final InstanceInfo instanceInfo = mock(InstanceInfo.class);
        doReturn(instanceInfo).when(applicationInfoManager).getInfo();
        dataCenterInfo = mock(DataCenterInfo.class);
        doReturn(dataCenterInfo).when(instanceInfo).getDataCenterInfo();
    }

    @Test
    void should_be_MyOwnEurekaNodeListProvider_when_dataCenter_is_MyOwn() {
        assertThatDelegateIsExactlyInstanceOf(MyOwn, MyOwnEurekaNodeListProvider.class);
    }

    @Test
    void should_be_EurekaNodeListProvider_when_dataCenter_is_Amazon() {
        assertThatDelegateIsExactlyInstanceOf(Amazon, EurekaNodeListProvider.class);
    }

    private void assertThatDelegateIsExactlyInstanceOf(DataCenterInfo.Name dataCenter, Class<? extends EVCacheNodeList> clazz) {
        doReturn(dataCenter).when(dataCenterInfo).getName();
        DataCenterAwareEurekaNodeListProvider provider = new DataCenterAwareEurekaNodeListProvider(applicationInfoManager, eurekaClient);
        assertThat(getField(provider, "delegate")).isExactlyInstanceOf(clazz);
    }
}
