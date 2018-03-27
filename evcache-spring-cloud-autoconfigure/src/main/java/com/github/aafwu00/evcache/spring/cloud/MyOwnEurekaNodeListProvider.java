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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ChainedDynamicProperty;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;
import com.netflix.evcache.util.EVCacheConfig;
import com.netflix.spectator.api.Id;

import static com.netflix.config.ConfigurationManager.getDeploymentContext;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * {@link EVCacheNodeList} implementation defaults to be Eureka based for MyOWN DataCenter.
 *
 * @author Taeho Kim
 * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
 */
public class MyOwnEurekaNodeListProvider implements EVCacheNodeList {
    private static final int DEFAULT_EVCACHE_PORT = 11211;
    private static final String DEFAULT_GROUP_NAME = "Default";
    private static final String DEFAULT_ZONE = "UNKNOWN";
    private final ApplicationInfoManager applicationInfoManager;
    private final EurekaClient eurekaClient;

    public MyOwnEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager,
                                       final EurekaClient eurekaClient) {
        this.applicationInfoManager = requireNonNull(applicationInfoManager);
        this.eurekaClient = requireNonNull(eurekaClient);
    }

    @Override
    public Map<ServerGroup, EVCacheServerGroupConfig> discoverInstances(final String appName) {
        if (applicationInfoManager.getInfo().getStatus() == InstanceInfo.InstanceStatus.DOWN) {
            return emptyMap();
        }
        final Application app = eurekaClient.getApplication(appName);
        if (isNull(app)) {
            return emptyMap();
        }
        final Map<ServerGroup, EVCacheServerGroupConfig> result = new ConcurrentHashMap<>();
        for (final InstanceInfo instanceInfo : app.getInstances()) {
            if (isAvailable(appName, instanceInfo)) {
                addInstance(result, appName, instanceInfo);
            }
        }
        return result;
    }

    private DynamicStringProperty ignoreHost(final String appName) {
        return EVCacheConfig.getInstance().getDynamicStringProperty(appName + ".ignore.hosts", "");
    }

    private boolean isAvailable(final String appName, final InstanceInfo instanceInfo) {
        return isAvailableStatus(instanceInfo)
            && isNotIgnoreHost(appName, instanceInfo)
            && isMyOwnDataCenter(instanceInfo);
    }

    private boolean isAvailableStatus(final InstanceInfo instanceInfo) {
        return nonNull(instanceInfo.getStatus())
            && InstanceInfo.InstanceStatus.OUT_OF_SERVICE != instanceInfo.getStatus()
            && InstanceInfo.InstanceStatus.DOWN != instanceInfo.getStatus();
    }

    private boolean isNotIgnoreHost(final String appName, final InstanceInfo instanceInfo) {
        final DynamicStringProperty ignoreHost = ignoreHost(appName);
        return !ignoreHost.get().contains(instanceInfo.getIPAddr())
            && !ignoreHost.get().contains(instanceInfo.getHostName());
    }

    private boolean isMyOwnDataCenter(final InstanceInfo instanceInfo) {
        return isNull(instanceInfo.getDataCenterInfo())
            || DataCenterInfo.Name.MyOwn == instanceInfo.getDataCenterInfo().getName();
    }

    private void addInstance(final Map<ServerGroup, EVCacheServerGroupConfig> result,
                             final String appName,
                             final InstanceInfo instanceInfo) {
        final int rendPort = rendPort(instanceInfo);
        final int port = port(appName, instanceInfo, rendPort);
        final ServerGroup serverGroup = serverGroup(instanceInfo);
        result.computeIfAbsent(serverGroup, key -> createServerGroupConfig(key, appName, instanceInfo, rendPort, port));
        result.get(serverGroup)
              .getInetSocketAddress()
              .add(address(instanceInfo, port));
    }

    private String groupName(final InstanceInfo instanceInfo) {
        return defaultIfBlank(getString(instanceInfo.getMetadata(), "evcache.group"),
                              defaultString(instanceInfo.getASGName(), DEFAULT_GROUP_NAME));
    }

    private String getString(final Map<String, String> metadata, final String key) {
        if (isNull(metadata)) {
            return "";
        }
        return metadata.getOrDefault(key, "");
    }

    private int rendPort(final InstanceInfo instanceInfo) {
        return getIntValue(instanceInfo.getMetadata(), "rend.port", 0);
    }

    private int port(final String appName, final InstanceInfo instanceInfo, final int rendPort) {
        final int evcachePort = evcachePort(instanceInfo);
        if (rendPort == 0) {
            return evcachePort;
        }
        if (useBatchPort(appName).get()) {
            return rendBatchPort(instanceInfo);
        }
        return rendPort;
    }

    private int evcachePort(final InstanceInfo instanceInfo) {
        return getIntValue(instanceInfo.getMetadata(), "evcache.port", DEFAULT_EVCACHE_PORT);
    }

    private int rendBatchPort(final InstanceInfo instanceInfo) {
        return getIntValue(instanceInfo.getMetadata(), "rend.batch.port", 0);
    }

    private ChainedDynamicProperty.BooleanProperty useBatchPort(final String appName) {
        return EVCacheConfig.getInstance()
                            .getChainedBooleanProperty(appName + ".use.batch.port",
                                                       "evcache.use.batch.port",
                                                       Boolean.FALSE,
                                                       null);
    }

    private ServerGroup serverGroup(final InstanceInfo instanceInfo) {
        final String zone = zone(instanceInfo);
        final String groupName = groupName(instanceInfo);
        return new ServerGroup(zone, groupName);
    }

    private String zone(final InstanceInfo instanceInfo) {
        final String zone = getDeploymentContext().getValue(DeploymentContext.ContextKey.zone);
        if (isNotEmpty(zone)) {
            return zone;
        }
        if (!isEmpty(instanceInfo.getMetadata())) {
            final String availabilityZone = instanceInfo.getMetadata().get("zone");
            if (isNotEmpty(availabilityZone)) {
                return availabilityZone;
            }
        }
        return DEFAULT_ZONE;
    }

    private EVCacheServerGroupConfig createServerGroupConfig(final ServerGroup serverGroup,
                                                             final String appName,
                                                             final InstanceInfo instanceInfo,
                                                             final int rendPort,
                                                             final int port) {
        EVCacheMetricsFactory.getInstance().getRegistry().gauge(spectatorId(serverGroup, appName), port);
        return new EVCacheServerGroupConfig(serverGroup,
                                            new HashSet<>(),
                                            rendPort,
                                            udsproxyMemcachedPort(instanceInfo),
                                            udsproxyMementoPort(instanceInfo));
    }

    private Id spectatorId(final ServerGroup serverGroup, final String appName) {
        return EVCacheMetricsFactory.getInstance()
                                    .getRegistry()
                                    .createId(appName + "-port", "ServerGroup", serverGroup.getName(), "APP", appName);
    }

    private int udsproxyMemcachedPort(final InstanceInfo instanceInfo) {
        return getIntValue(instanceInfo.getMetadata(), "udsproxy.memcached.port", 0);
    }

    private int getIntValue(final Map<String, String> metadata, final String key, final int defaultValue) {
        if (isNull(metadata)) {
            return defaultValue;
        }
        return toInt(metadata.get(key), defaultValue);
    }

    private int udsproxyMementoPort(final InstanceInfo instanceInfo) {
        return getIntValue(instanceInfo.getMetadata(), "udsproxy.memento.port", 0);
    }

    private InetSocketAddress address(final InstanceInfo instanceInfo, final int port) {
        return new InetSocketAddress(instanceInfo.getHostName(), port);
    }
}
