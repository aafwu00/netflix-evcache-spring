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

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.AmazonInfo.MetaDataKey;
import com.netflix.appinfo.DataCenterInfo;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;

/**
 * EVCache Server Configuration that setting up {@link com.netflix.appinfo.EurekaInstanceConfig}.
 * <p>
 * look on {@code org.springframework.cloud.netflix.sidecar.SidecarConfiguration}
 *
 * @author Taeho Kim
 * @see EurekaClientAutoConfiguration
 * @see com.netflix.evcache.pool.DiscoveryNodeListProvider
 */
@Configuration
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class EVCacheServerAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EVCacheServerAutoConfiguration.class);
    private final ConfigurableEnvironment environment;
    private final InetUtils inetUtils;
    private final EurekaInstanceConfigBean eurekaInstanceConfigBean;

    public EVCacheServerAutoConfiguration(final ConfigurableEnvironment environment,
                                          final InetUtils inetUtils,
                                          final EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        this.environment = requireNonNull(environment);
        this.inetUtils = requireNonNull(inetUtils);
        this.eurekaInstanceConfigBean = requireNonNull(eurekaInstanceConfigBean);
    }

    @Bean
    public HasFeatures evcacheServerFeature() {
        return HasFeatures.namedFeature("EVCache Server", EVCacheServerAutoConfiguration.class);
    }

    @PostConstruct
    public void initialize() {
        handleEureka();
    }

    private void handleEureka() {
        handleEurekaASGName();
        handleEurekaDataCenter();
        handleEurekaMetadata();
    }

    private void handleEurekaASGName() {
        if (isNotBlank(eurekaInstanceConfigBean.getASGName())) {
            return;
        }
        eurekaInstanceConfigBean.setASGName(environment.getProperty("evcache.asg-name", "DEFAULT"));
        LOGGER.warn("eureka ASG Name is Missing, set `{}`", eurekaInstanceConfigBean.getASGName());
    }

    private void handleEurekaDataCenter() {
        if (isAmazonDataCenter()) {
            return;
        }
        final AmazonInfo amazonInfo = createAmazonInfo();
        eurekaInstanceConfigBean.setDataCenterInfo(amazonInfo);
        LOGGER.warn("DataCenter is Not Amazon, To changed, `{}`", amazonInfo);
    }

    private boolean isAmazonDataCenter() {
        final DataCenterInfo dataCenter = eurekaInstanceConfigBean.getDataCenterInfo();
        return DataCenterInfo.Name.Amazon == dataCenter.getName() && (dataCenter instanceof AmazonInfo);
    }

    private AmazonInfo createAmazonInfo() {
        final InetUtils.HostInfo hostInfo = inetUtils.findFirstNonLoopbackHostInfo();
        final AmazonInfo.Builder builder = AmazonInfo.Builder.newBuilder();
        addMetadata(builder, MetaDataKey.availabilityZone, DEFAULT_ZONE);
        addMetadata(builder, MetaDataKey.amiId, "n/a");
        addMetadata(builder, MetaDataKey.instanceId, eurekaInstanceConfigBean.getInstanceId());
        addMetadata(builder, MetaDataKey.publicHostname, hostInfo.getHostname());
        addMetadata(builder, MetaDataKey.publicIpv4, hostInfo.getIpAddress());
        return builder.build();
    }

    private void addMetadata(final AmazonInfo.Builder builder, final MetaDataKey key, final String defaultValue) {
        builder.addMetadata(key, environment.getProperty("evcache." + key.getName(), defaultValue));
    }

    private void handleEurekaMetadata() {
        final Map<String, String> metadata = eurekaInstanceConfigBean.getMetadataMap();
        putIfAbsentWhenContainProperty(metadata, "evcache.port");
        putIfAbsentWhenContainProperty(metadata, "evcache.secure.port");
        putIfAbsentWhenContainProperty(metadata, "rend.port");
        putIfAbsentWhenContainProperty(metadata, "rend.batch.port");
        putIfAbsentWhenContainProperty(metadata, "udsproxy.memcached.port");
        putIfAbsentWhenContainProperty(metadata, "udsproxy.memento.port");
    }

    private void putIfAbsentWhenContainProperty(final Map<String, String> metadata, final String key) {
        if (!environment.containsProperty(key)) {
            return;
        }
        final String value = metadata.putIfAbsent(key, environment.getProperty(key));
        LOGGER.info("eureka metadata set key:`{}`, value:`{}`", key, value);
    }
}
