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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static java.util.Objects.requireNonNull;

/**
 * EVCache Server Configuration that setting up {@link com.netflix.appinfo.EurekaInstanceConfig}.
 * <p>
 * look on {@code org.springframework.cloud.netflix.sidecar.SidecarConfiguration}
 * look on {@code com.github.aafwu00.spring.cloud.evcache.client.MyOwnEurekaNodeListProvider}
 *
 * @author Taeho Kim
 * @see EurekaClientAutoConfiguration
 * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
 */
@Configuration
@ConditionalOnProperty(value = "evcar.enabled", matchIfMissing = true)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
@EnableConfigurationProperties(EVCacheServerProperties.class)
public class EVCacheServerAutoConfiguration {
    private final ConfigurableEnvironment environment;
    private final EurekaInstanceConfigBean eurekaInstanceConfigBean;
    private final EVCacheServerProperties properties;

    public EVCacheServerAutoConfiguration(final ConfigurableEnvironment environment,
                                          final EurekaInstanceConfigBean eurekaInstanceConfigBean,
                                          final EVCacheServerProperties properties) {
        this.environment = requireNonNull(environment);
        this.eurekaInstanceConfigBean = requireNonNull(eurekaInstanceConfigBean);
        this.properties = requireNonNull(properties);
    }

    @Bean
    public HasFeatures evcarFeature() {
        return HasFeatures.namedFeature("EVCache Server", EVCacheServerAutoConfiguration.class);
    }

    @PostConstruct
    public void init() {
        final Map<String, String> metadataMap = eurekaInstanceConfigBean.getMetadataMap();
        putIfAbsent(metadataMap, "evcache.port", Integer.toString(properties.getPort()));
        putIfAbsent(metadataMap, "evcache.group", properties.getGroup());
        putIfAbsentWhenContainProperty(metadataMap, "rend.port");
        putIfAbsentWhenContainProperty(metadataMap, "rend.batch.port");
        putIfAbsentWhenContainProperty(metadataMap, "udsproxy.memcached.port");
        putIfAbsentWhenContainProperty(metadataMap, "udsproxy.memento.port");
    }

    private void putIfAbsent(final Map<String, String> metadataMap, final String key, final String defaultValue) {
        metadataMap.putIfAbsent(key, environment.getProperty(key, defaultValue));
    }

    private void putIfAbsentWhenContainProperty(final Map<String, String> metadataMap, final String key) {
        if (environment.containsProperty(key)) {
            metadataMap.putIfAbsent(key, environment.getProperty(key));
        }
    }
}
