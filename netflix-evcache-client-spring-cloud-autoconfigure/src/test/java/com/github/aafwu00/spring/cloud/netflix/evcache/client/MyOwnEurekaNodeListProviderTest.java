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

package com.github.aafwu00.spring.cloud.netflix.evcache.client;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.evcache.pool.ServerGroup;
import com.netflix.evcache.util.EVCacheConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class MyOwnEurekaNodeListProviderTest {
    private ApplicationInfoManager applicationInfoManager;
    private EurekaClient eurekaClient;
    private InstanceInfo instanceInfo;
    private InstanceInfo otherInstanceInfo;
    private Application application;
    private MyOwnEurekaNodeListProvider provider;

    @BeforeEach
    void setUp() {
        applicationInfoManager = mock(ApplicationInfoManager.class);
        eurekaClient = mock(EurekaClient.class);
        instanceInfo = mock(InstanceInfo.class);
        otherInstanceInfo = mock(InstanceInfo.class);
        application = mock(Application.class);
        provider = new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
        doReturn(instanceInfo).when(applicationInfoManager).getInfo();
    }

    @Test
    void name() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
        doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(otherInstanceInfo).getDataCenterInfo();
        doReturn("notmatch").when(otherInstanceInfo).getHostName();
        doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
        assertThat(provider.discoverInstances("test")).containsKeys(new ServerGroup("UNKNOWN", "Default"));
    }

    @Test
    void should_be_empty_when_instance_is_down() {
        doReturn(InstanceInfo.InstanceStatus.DOWN).when(instanceInfo).getStatus();
        assertThat(provider.discoverInstances("test")).isEmpty();
    }

    @Test
    void should_be_empty_when_app_is_null() {
        doReturn(null).when(eurekaClient).getApplication("test");
        assertThat(provider.discoverInstances("test")).isEmpty();
    }

    @Test
    void should_be_empty_when_instance_is_not_available() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        assertAll(
            () -> {
                doReturn(null).when(otherInstanceInfo).getStatus();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.DOWN).when(otherInstanceInfo).getStatus();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.OUT_OF_SERVICE).when(otherInstanceInfo).getStatus();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                EVCacheConfig.getInstance().getDynamicStringProperty("test" + ".ignore.hosts", "server1,server2");
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn("server1").when(otherInstanceInfo).getIPAddr();
                doReturn(AmazonInfo.Builder.newBuilder().build()).when(otherInstanceInfo).getDataCenterInfo();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                EVCacheConfig.getInstance().getDynamicStringProperty("test" + ".ignore.hosts", "server1,server2");
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn("server1").when(otherInstanceInfo).getHostName();
                doReturn(AmazonInfo.Builder.newBuilder().build()).when(otherInstanceInfo).getDataCenterInfo();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn(AmazonInfo.Builder.newBuilder().build()).when(otherInstanceInfo).getDataCenterInfo();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(otherInstanceInfo).getDataCenterInfo();
                assertThat(provider.discoverInstances("test")).isEmpty();
            }
        );
    }
}
