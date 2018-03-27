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

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class MyOwnEurekaNodeListProviderTest {
    private EurekaClient eurekaClient;
    private InstanceInfo instanceInfo;
    private InstanceInfo otherInstanceInfo;
    private Application application;
    private MyOwnEurekaNodeListProvider provider;

    @BeforeEach
    void setUp() {
        eurekaClient = mock(EurekaClient.class);
        instanceInfo = mock(InstanceInfo.class);
        otherInstanceInfo = mock(InstanceInfo.class);
        application = mock(Application.class);
        final ApplicationInfoManager applicationInfoManager = mock(ApplicationInfoManager.class);
        provider = new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
        doReturn(instanceInfo).when(applicationInfoManager).getInfo();
        ConfigurationManager.getConfigInstance().setProperty("test" + ".ignore.hosts", "server1,server2");
        com.netflix.config.ConfigurationManager.getDeploymentContext().setValue(DeploymentContext.ContextKey.zone, "");
    }

    @Test
    void should_be_contain_default_server_group() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
        doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(otherInstanceInfo).getDataCenterInfo();
        doReturn("notmatch").when(otherInstanceInfo).getHostName();
        doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
        doReturn(null).when(otherInstanceInfo).getMetadata();
        final Map<ServerGroup, EVCacheServerGroupConfig> result = provider.discoverInstances("test");
        final ServerGroup key = new ServerGroup("UNKNOWN", "Default");
        assertAll(
            () -> assertThat(result).hasSize(1).containsKey(key),
            () -> assertThat(result.get(key).getServerGroup()).isEqualTo(key),
            () -> assertThat(result.get(key).getInetSocketAddress()).containsOnly(new InetSocketAddress("notmatch", 11211)),
            () -> assertThat(result.get(key).getRendPort()).isEqualTo(0),
            () -> assertThat(result.get(key).getUdsproxyMemcachedPort()).isEqualTo(0),
            () -> assertThat(result.get(key).getUpdsproxyMememtoPort()).isEqualTo(0)
        );
    }

    @Test
    void should_be_contain_zone_server_group_when_otherInstanceInfo_dataCenterInfo_is_null() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
        doReturn(null).when(otherInstanceInfo).getDataCenterInfo();
        doReturn("notmatch").when(otherInstanceInfo).getHostName();
        doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
        doReturn(null).when(otherInstanceInfo).getMetadata();
        com.netflix.config.ConfigurationManager.getDeploymentContext().setValue(DeploymentContext.ContextKey.zone, "us-east-1");
        final Map<ServerGroup, EVCacheServerGroupConfig> result = provider.discoverInstances("test");
        final ServerGroup key = new ServerGroup("us-east-1", "Default");
        assertAll(
            () -> assertThat(result).hasSize(1).containsKey(key),
            () -> assertThat(result.get(key).getServerGroup()).isEqualTo(key),
            () -> assertThat(result.get(key).getInetSocketAddress()).containsOnly(new InetSocketAddress("notmatch", 11211)),
            () -> assertThat(result.get(key).getRendPort()).isEqualTo(0),
            () -> assertThat(result.get(key).getUdsproxyMemcachedPort()).isEqualTo(0),
            () -> assertThat(result.get(key).getUpdsproxyMememtoPort()).isEqualTo(0)
        );
    }

    @Test
    void should_be_contain_use_rend_port_server() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
        doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(otherInstanceInfo).getDataCenterInfo();
        doReturn("notmatch").when(otherInstanceInfo).getHostName();
        doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
        final Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put("zone", "rend");
        metaInfo.put("evcache.port", "11211");
        metaInfo.put("evcache.group", "group1");
        metaInfo.put("rend.batch.port", "11213");
        metaInfo.put("rend.port", "11212");
        metaInfo.put("udsproxy.memento.port", "11215");
        doReturn(metaInfo).when(otherInstanceInfo).getMetadata();
        ConfigurationManager.getConfigInstance().setProperty("evcache.use.batch.port", Boolean.FALSE);
        final Map<ServerGroup, EVCacheServerGroupConfig> result = provider.discoverInstances("test");
        final ServerGroup key = new ServerGroup("rend", "group1");
        assertAll(
            () -> assertThat(result).hasSize(1).containsKey(key),
            () -> assertThat(result.get(key).getServerGroup()).isEqualTo(key),
            () -> assertThat(result.get(key).getInetSocketAddress()).containsOnly(new InetSocketAddress("notmatch", 11212)),
            () -> assertThat(result.get(key).getRendPort()).isEqualTo(11212),
            () -> assertThat(result.get(key).getUdsproxyMemcachedPort()).isEqualTo(0),
            () -> assertThat(result.get(key).getUpdsproxyMememtoPort()).isEqualTo(11215)
        );
    }

    @Test
    void should_be_contain_use_rend_batch_port_server() {
        doReturn(InstanceInfo.InstanceStatus.UP).when(instanceInfo).getStatus();
        doReturn(application).when(eurekaClient).getApplication("test");
        doReturn(Collections.singletonList(otherInstanceInfo)).when(application).getInstances();
        doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
        doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(otherInstanceInfo).getDataCenterInfo();
        doReturn("notmatch").when(otherInstanceInfo).getHostName();
        doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
        doReturn("group2").when(otherInstanceInfo).getASGName();
        final Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put("rend.port", "11212");
        metaInfo.put("rend.batch.port", "11213");
        metaInfo.put("udsproxy.memcached.port", "11214");
        doReturn(metaInfo).when(otherInstanceInfo).getMetadata();
        ConfigurationManager.getConfigInstance().setProperty("test.use.batch.port", Boolean.TRUE);
        final Map<ServerGroup, EVCacheServerGroupConfig> result = provider.discoverInstances("test");
        final ServerGroup key = new ServerGroup("UNKNOWN", "group2");
        assertAll(
            () -> assertThat(result).hasSize(1).containsKey(key),
            () -> assertThat(result.get(key).getServerGroup()).isEqualTo(key),
            () -> assertThat(result.get(key).getInetSocketAddress()).containsOnly(new InetSocketAddress("notmatch", 11213)),
            () -> assertThat(result.get(key).getRendPort()).isEqualTo(11212),
            () -> assertThat(result.get(key).getUdsproxyMemcachedPort()).isEqualTo(11214),
            () -> assertThat(result.get(key).getUpdsproxyMememtoPort()).isEqualTo(0)
        );
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
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn("server1").when(otherInstanceInfo).getIPAddr();
                doReturn("notmatch").when(otherInstanceInfo).getHostName();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
                doReturn("server2").when(otherInstanceInfo).getHostName();
                assertThat(provider.discoverInstances("test")).isEmpty();
            },
            () -> {
                doReturn(InstanceInfo.InstanceStatus.UP).when(otherInstanceInfo).getStatus();
                doReturn("notmatch").when(otherInstanceInfo).getIPAddr();
                doReturn("notmatch").when(otherInstanceInfo).getHostName();
                doReturn(AmazonInfo.Builder.newBuilder().build()).when(otherInstanceInfo).getDataCenterInfo();
                assertThat(provider.discoverInstances("test")).isEmpty();
            }
        );
    }
}
