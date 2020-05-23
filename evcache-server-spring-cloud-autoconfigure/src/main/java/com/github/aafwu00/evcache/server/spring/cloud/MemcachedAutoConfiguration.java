/*
 * Copyright 2017-2020 the original author or authors.
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

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

import static com.netflix.evcache.pool.eureka.EurekaNodeListProvider.DEFAULT_PORT;
import static com.netflix.evcache.pool.eureka.EurekaNodeListProvider.DEFAULT_SECURE_PORT;
import static java.lang.Boolean.FALSE;

/**
 * Memcached Configuration look on {@code org.springframework.cloud.netflix.sidecar.SidecarConfiguration}
 *
 * @author Taeho Kim
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@ConditionalOnEnabledHealthIndicator("memcached")
@AutoConfigureAfter(EVCacheServerAutoConfiguration.class)
public class MemcachedAutoConfiguration {
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Bean
    @ConditionalOnMissingBean(MemcachedClient.class)
    public MemcachedClient memcachedClient(final EurekaInstanceConfigBean eurekaInstanceConfigBean,
                                           final ConfigurableEnvironment environment) throws Exception {
        final MemcachedClientFactoryBean factory = new MemcachedClientFactoryBean();
        factory.setServers(eurekaInstanceConfigBean.getHostname() + ":" + port(environment, eurekaInstanceConfigBean));
        return (MemcachedClient) factory.getObject();
    }

    /**
     * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
     */
    private int port(final ConfigurableEnvironment environment,
                     final EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        final Map<String, String> metaInfo = eurekaInstanceConfigBean.getMetadataMap();
        if (isSecure(environment, eurekaInstanceConfigBean)) {
            return evcacheSecurePort(metaInfo);
        }
        if (!hasRendPort(metaInfo)) {
            return evcachePort(metaInfo);
        }
        if (useBatchPort(environment, eurekaInstanceConfigBean)) {
            return rendBatchPort(metaInfo);
        }
        return rendPort(metaInfo);
    }

    private Boolean isSecure(final ConfigurableEnvironment environment,
                             final EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        return getProperty(environment,
                           eurekaInstanceConfigBean.getASGName() + ".use.secure",
                           eurekaInstanceConfigBean.getAppname() + ".use.secure");
    }

    private int evcacheSecurePort(final Map<String, String> metaInfo) {
        return toInt(metaInfo, "evcache.secure.port", DEFAULT_SECURE_PORT);
    }

    private boolean hasRendPort(final Map<String, String> metaInfo) {
        return metaInfo.containsKey("rend.port");
    }

    private int evcachePort(final Map<String, String> metaInfo) {
        return toInt(metaInfo, "evcache.port", DEFAULT_PORT);
    }

    private Boolean useBatchPort(final ConfigurableEnvironment environment,
                                 final EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        return getProperty(environment,
                           eurekaInstanceConfigBean.getAppname() + ".use.batch.port",
                           "evcache.use.batch.port");
    }

    private int rendBatchPort(final Map<String, String> metaInfo) {
        return toInt(metaInfo, "rend.batch.port", "0");
    }

    private int rendPort(final Map<String, String> metaInfo) {
        return toInt(metaInfo, "rend.port", "0");
    }

    private Boolean getProperty(final ConfigurableEnvironment environment,
                                final String firstKey,
                                final String secondKey) {
        return getProperty(environment, firstKey, getProperty(environment, secondKey, FALSE));
    }

    private Boolean getProperty(final ConfigurableEnvironment environment,
                                final String key,
                                final Boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private int toInt(final Map<String, String> metadata, final String key, final String defaultValue) {
        return NumberUtils.toInt(metadata.getOrDefault(key, defaultValue));
    }
}
