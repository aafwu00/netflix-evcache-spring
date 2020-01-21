/*
 * Copyright 2017-2019 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.AmazonInfo.MetaDataKey;
import com.netflix.appinfo.DataCenterInfo;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;
import static org.springframework.util.ClassUtils.isAssignableValue;

/**
 * EVCache Server Configuration that setting up {@link com.netflix.appinfo.EurekaInstanceConfig}.
 *
 * @author Taeho Kim
 * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
 */
class EVCacheServerEurekaInstanceConfigBeanPostProcessor implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EVCacheServerEurekaInstanceConfigBeanPostProcessor.class);
    private final ConfigurableEnvironment environment;
    private final EurekaClientConfigBean eurekaClientConfigBean;

    EVCacheServerEurekaInstanceConfigBeanPostProcessor(final ConfigurableEnvironment environment,
                                                       final EurekaClientConfigBean eurekaClientConfigBean) {
        this.environment = requireNonNull(environment);
        this.eurekaClientConfigBean = requireNonNull(eurekaClientConfigBean);
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (!isAssignableValue(EurekaInstanceConfigBean.class, bean)) {
            return bean;
        }
        final EurekaInstanceConfigBean config = (EurekaInstanceConfigBean) bean;
        if (isAmazonDataCenter(config)) {
            return bean;
        }
        final AmazonInfo amazonInfo = createAmazonInfo(config);
        config.setDataCenterInfo(amazonInfo);
        LOGGER.warn("DataCenter changed. {}", amazonInfo);
        return bean;
    }

    private boolean isAmazonDataCenter(final EurekaInstanceConfigBean config) {
        final DataCenterInfo dataCenter = config.getDataCenterInfo();
        return DataCenterInfo.Name.Amazon == dataCenter.getName() && (dataCenter instanceof AmazonInfo);
    }

    private AmazonInfo createAmazonInfo(final EurekaInstanceConfigBean config) {
        final AmazonInfo.Builder builder = AmazonInfo.Builder.newBuilder();
        addMetadata(builder, MetaDataKey.availabilityZone, availabilityZone());
        addMetadata(builder, MetaDataKey.amiId, "n/a");
        addMetadata(builder, MetaDataKey.instanceId, config.getInstanceId());
        addMetadata(builder, MetaDataKey.publicHostname, config.getHostname());
        addMetadata(builder, MetaDataKey.publicIpv4, config.getIpAddress());
        addMetadata(builder, MetaDataKey.localIpv4, config.getIpAddress());
        return builder.build();
    }

    private String availabilityZone() {
        final String[] availabilityZones = eurekaClientConfigBean.getAvailabilityZones(eurekaClientConfigBean.getRegion());
        if (isNotEmpty(availabilityZones)) {
            return availabilityZones[0];
        }
        return DEFAULT_ZONE;
    }

    private void addMetadata(final AmazonInfo.Builder builder, final MetaDataKey key, final String defaultValue) {
        builder.addMetadata(key, environment.getProperty("evcache." + key.getName(), defaultValue));
    }
}
