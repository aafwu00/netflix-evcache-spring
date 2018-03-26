package com.github.aafwu00.spring.cloud.netflix.evcache.client;

import static java.util.Collections.emptyMap;

import com.google.common.net.InetAddresses;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ChainedDynamicProperty;
import com.netflix.config.DynamicStringSetProperty;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;
import com.netflix.evcache.util.EVCacheConfig;
import com.netflix.spectator.api.Id;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kth on 2018. 3. 22..
 */
public class AwsEurekaNodeListProvider implements EVCacheNodeList {
    public static final String DEFAULT_PORT = "11211";
    public static final String DEFAULT_SECURE_PORT = "11443";

    private final EurekaClient eurekaClient;
    private final ApplicationInfoManager applicationInfoManager;
    private final Map<String, ChainedDynamicProperty.BooleanProperty> useRendBatchPortMap = new HashMap<String, ChainedDynamicProperty.BooleanProperty>();
    private final DynamicStringSetProperty ignoreHosts;


    public AwsEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient, final String appName) {
        this.applicationInfoManager = applicationInfoManager;
        this.eurekaClient = eurekaClient;
        ignoreHosts = new DynamicStringSetProperty(appName + ".ignore.hosts", "");
    }


    @Override
    public Map<ServerGroup, EVCacheServerGroupConfig> discoverInstances(final String appName) throws IOException {

        if ((applicationInfoManager.getInfo().getStatus() == InstanceInfo.InstanceStatus.DOWN)) {
            return emptyMap();
        }

        final Application app = eurekaClient.getApplication(appName);
        if (app == null) return emptyMap();

        final Map<ServerGroup, EVCacheServerGroupConfig> result = new ConcurrentHashMap<ServerGroup, EVCacheServerGroupConfig>();

        for (final InstanceInfo instanceInfo : app.getInstances()) {
            if (isAvailable(appName, instanceInfo)) {
                addInstance(result, appName, instanceInfo);
            }
        }
        return result;
    }

    private boolean isAvailable(final String appName, final InstanceInfo instanceInfo) {
        return isAvailableStatus(instanceInfo)
                && isAmazon(instanceInfo)
                && isNotIgnoreHost(instanceInfo);

    }

    private boolean isAvailableStatus(final InstanceInfo instanceInfo) {
        return instanceInfo.getStatus() != null
                && InstanceInfo.InstanceStatus.OUT_OF_SERVICE != instanceInfo.getStatus()
                && InstanceInfo.InstanceStatus.DOWN != instanceInfo.getStatus();
    }


    private boolean isAmazon(final InstanceInfo instanceInfo) {

        final DataCenterInfo dcInfo = instanceInfo.getDataCenterInfo();
        final AmazonInfo amznInfo = (AmazonInfo) dcInfo;

        return (dcInfo != null)
                && (DataCenterInfo.Name.Amazon == dcInfo.getName() && (dcInfo instanceof AmazonInfo))
                && (amznInfo.get(AmazonInfo.MetaDataKey.availabilityZone) != null)
                && (instanceInfo.getASGName() != null);
    }

    private boolean isNotIgnoreHost(final InstanceInfo instanceInfo) {
        final AmazonInfo amznInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();
        final String host = amznInfo.get(AmazonInfo.MetaDataKey.publicHostname);
        final String localIp = amznInfo.get(AmazonInfo.MetaDataKey.localIpv4);
        return (localIp != null && !ignoreHosts.get().contains(localIp))
                && (host != null && !ignoreHosts.get().contains(host));
    }

    private void addInstance(final Map<ServerGroup, EVCacheServerGroupConfig> instancesSpecific,
                             final String appName,
                             final InstanceInfo instanceInfo) throws IOException {

        final AmazonInfo amznInfo = (AmazonInfo) instanceInfo.getDataCenterInfo();

        final String zone = amznInfo.get(AmazonInfo.MetaDataKey.availabilityZone);
        final String asgName = instanceInfo.getASGName();
        final Map<String, String> metaInfo = instanceInfo.getMetadata();

        final int rendPort = (metaInfo != null && metaInfo.containsKey("rend.port")) ? Integer.parseInt(metaInfo.get("rend.port")) : 0;
        int port = getPort(rendPort, metaInfo, asgName, appName);

        final ServerGroup serverGroup = new ServerGroup(zone, asgName);


        final Set<InetSocketAddress> instances;
        final EVCacheServerGroupConfig config;
        if (instancesSpecific.containsKey(serverGroup)) {
            config = instancesSpecific.get(serverGroup);
            instances = config.getInetSocketAddress();
        } else {
            instances = new HashSet<InetSocketAddress>();
            config = createServerGroupConfig(serverGroup, appName, instances, instanceInfo, rendPort, port);
            instancesSpecific.put(serverGroup, config);
        }

        boolean isInCloud = isInCloud();
        instances.add(getAddress(amznInfo, port, isInCloud));

    }

    private int getPort(final int rendPort, final Map<String, String> metaInfo, final String asgName, final String appName) {

        final int evcachePort = Integer.parseInt((metaInfo != null && metaInfo.containsKey("evcache.port")) ? metaInfo.get("evcache.port") : DEFAULT_PORT);
        final int rendBatchPort = (metaInfo != null && metaInfo.containsKey("rend.batch.port")) ? Integer.parseInt(metaInfo.get("rend.batch.port")) : 0;

        ChainedDynamicProperty.BooleanProperty useBatchPort = useRendBatchPortMap.get(asgName);
        if (useBatchPort == null) {
            useBatchPort = EVCacheConfig.getInstance().getChainedBooleanProperty(appName + ".use.batch.port", "evcache.use.batch.port", Boolean.FALSE, null);
            useRendBatchPortMap.put(asgName, useBatchPort);
        }

        int port = rendPort == 0 ? evcachePort : ((useBatchPort.get().booleanValue()) ? rendBatchPort : rendPort);
        final ChainedDynamicProperty.BooleanProperty isSecure = EVCacheConfig.getInstance().getChainedBooleanProperty(asgName + ".use.secure", appName + ".use.secure", false, null);
        if (isSecure.get()) {
            port = Integer.parseInt((metaInfo != null && metaInfo.containsKey("evcache.secure.port")) ? metaInfo.get("evcache.secure.port") : DEFAULT_SECURE_PORT);
        }

        return port;

    }

    private EVCacheServerGroupConfig createServerGroupConfig(final ServerGroup serverGroup,
                                                             final String appName,
                                                             final Set<InetSocketAddress> instances,
                                                             final InstanceInfo instanceInfo,
                                                             final int rendPort,
                                                             final int port) {
        EVCacheMetricsFactory.getInstance().getRegistry().gauge(spectatorId(serverGroup, appName), port);

        final Map<String, String> metaInfo = instanceInfo.getMetadata();
        final int udsproxyMemcachedPort = (metaInfo != null && metaInfo.containsKey("udsproxy.memcached.port")) ? Integer.parseInt(metaInfo.get("udsproxy.memcached.port")) : 0;
        final int udsproxyMementoPort = (metaInfo != null && metaInfo.containsKey("udsproxy.memento.port")) ? Integer.parseInt(metaInfo.get("udsproxy.memento.port")) : 0;

        return new EVCacheServerGroupConfig(serverGroup, instances, rendPort, udsproxyMemcachedPort, udsproxyMementoPort);
    }


    private boolean isInCloud() {

        boolean isInCloud = false;

        final InstanceInfo myInfo = applicationInfoManager.getInfo();
        final DataCenterInfo myDC = myInfo.getDataCenterInfo();
        final AmazonInfo myAmznDC = (myDC instanceof AmazonInfo) ? (AmazonInfo) myDC : null;
        final String myIp = myInfo.getIPAddr();
        final String myInstanceId = myInfo.getInstanceId();
        final String myPublicHostName = (myAmznDC != null) ? myAmznDC.get(AmazonInfo.MetaDataKey.publicHostname) : null;
        if (myPublicHostName != null) {
            isInCloud = myPublicHostName.startsWith("ec2");
        }

        if (!isInCloud) {
            if (myAmznDC != null && myAmznDC.get(AmazonInfo.MetaDataKey.vpcId) != null) {
                isInCloud = true;
            } else {
                if (myIp.equals(myInstanceId)) {
                    isInCloud = false;
                }
            }
        }
        return isInCloud;
    }


    private Id spectatorId(final ServerGroup serverGroup, final String appName) {
        return EVCacheMetricsFactory.getInstance()
                .getRegistry()
                .createId(appName + "-port", "ServerGroup", serverGroup.getName(), "APP", appName);
    }

    private InetSocketAddress getAddress(final AmazonInfo amznInfo, final int port, final boolean isInCloud) throws IOException {

        final String host = amznInfo.get(AmazonInfo.MetaDataKey.publicHostname);
        InetSocketAddress address = null;
        final String vpcId = amznInfo.get(AmazonInfo.MetaDataKey.vpcId);
        final String localIp = amznInfo.get(AmazonInfo.MetaDataKey.localIpv4);

        if (vpcId != null) {
            final InetAddress add = InetAddresses.forString(localIp);
            final InetAddress inetAddress = InetAddress.getByAddress(localIp, add.getAddress());
            address = new InetSocketAddress(inetAddress, port);

        } else {
            if (host != null && host.startsWith("ec2")) {

                final InetAddress inetAddress = (localIp != null) ? InetAddress.getByAddress(host, InetAddresses.forString(localIp).getAddress()) : InetAddress.getByName(host);
                address = new InetSocketAddress(inetAddress, port);
            } else {
                final String ipToUse = (isInCloud) ? localIp : amznInfo.get(AmazonInfo.MetaDataKey.publicIpv4);
                final InetAddress add = InetAddresses.forString(ipToUse);
                final InetAddress inetAddress = InetAddress.getByAddress(ipToUse, add.getAddress());
                address = new InetSocketAddress(inetAddress, port);

            }
        }
        return address;

    }

}

