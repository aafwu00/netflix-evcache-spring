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

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

import static com.netflix.evcache.pool.eureka.EurekaNodeListProvider.DEFAULT_PORT;
import static com.netflix.evcache.pool.eureka.EurekaNodeListProvider.DEFAULT_SECURE_PORT;
import static java.lang.Boolean.FALSE;

/**
 * EVCache Server Health Configuration that setting up
 *
 * @author Taeho Kim
 */
@Configuration
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@ConditionalOnProperty(value = "evcache.server.health.enabled", matchIfMissing = true)
@AutoConfigureAfter(EVCacheServerAutoConfiguration.class)
public class EVCacheServerHealthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MemcachedClient.class)
    public MemcachedClientFactoryBean memcachedClient(final EurekaInstanceConfigBean eurekaInstanceConfigBean,
                                                      final ConfigurableEnvironment environment) {
        final MemcachedClientFactoryBean bean = new MemcachedClientFactoryBean();
        bean.setServers(eurekaInstanceConfigBean.getHostname() + ":" + port(environment, eurekaInstanceConfigBean));
        return bean;
    }

    /**
     * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
     */
    private int port(final ConfigurableEnvironment environment,
                     final EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        final Map<String, String> metaInfo = eurekaInstanceConfigBean.getMetadataMap();
        final Boolean isSecure = getProperty(environment,
                                             eurekaInstanceConfigBean.getASGName() + ".use.secure",
                                             eurekaInstanceConfigBean.getAppname() + ".use.secure");
        if (isSecure) {
            return toInt(metaInfo, "evcache.secure.port", DEFAULT_SECURE_PORT);
        }
        final int rendPort = toInt(metaInfo, "rend.port", "0");
        if (rendPort == 0) {
            return toInt(metaInfo, "evcache.port", DEFAULT_PORT);
        }
        final Boolean useBatchPort = getProperty(environment,
                                                 eurekaInstanceConfigBean.getAppname() + ".use.batch.port",
                                                 "evcache.use.batch.port");
        if (useBatchPort) {
            return toInt(metaInfo, "rend.batch.port", "0");
        }
        return rendPort;
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

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.eureka.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedHealthCheckHandler memcachedHealthCheckHandler(final MemcachedClient client) {
        return new MemcachedHealthCheckHandler(client);
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.memcached.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedHealthIndicator memcachedHealthIndicator(final MemcachedClient client) {
        return new MemcachedHealthIndicator(client);
    }
}
