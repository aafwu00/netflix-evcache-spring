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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.github.aafwu00.evcache.spring.boot.ConditionalOnEVCache;
import com.github.aafwu00.evcache.spring.boot.EVCacheAutoConfiguration;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.EVCache;
import com.netflix.evcache.connection.ConnectionFactoryBuilder;
import com.netflix.evcache.connection.IConnectionBuilder;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.SimpleNodeListProvider;

/**
 * Spring configuration for configuring EVCache defaults to be Eureka based
 *
 * @author Taeho Kim
 * @see ArchaiusAutoConfiguration
 * @see EurekaClientAutoConfiguration
 * @see EVCacheAutoConfiguration
 */
@Configuration
@ConditionalOnProperty(value = "evcache.cloud.enabled", matchIfMissing = true)
@ConditionalOnEVCache
@AutoConfigureAfter({ArchaiusAutoConfiguration.class, EurekaClientAutoConfiguration.class})
@AutoConfigureBefore(EVCacheAutoConfiguration.class)
@EnableConfigurationProperties
@PropertySource("classpath:evcache/evcache.properties")
public class EVCacheCloudAutoConfiguration {
    @Bean
    public HasFeatures evcacheCloudClientFeature() {
        return HasFeatures.namedFeature("Netflix EVCache Cloud Client", EVCache.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public IConnectionBuilder connectionBuilder() {
        return new ConnectionFactoryBuilder();
    }

    @Bean
    @ConditionalOnMissingBean({ApplicationInfoManager.class, EurekaClient.class, EVCacheNodeList.class})
    public EVCacheNodeList simpleNodeListProvider() {
        return new SimpleNodeListProvider();
    }

    @Bean
    @ConditionalOnBean({ApplicationInfoManager.class, EurekaClient.class})
    @ConditionalOnMissingBean(EVCacheNodeList.class)
    public EVCacheNodeList myOwnEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager,
                                                       final EurekaClient eurekaClient) {
        return new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public EVCacheClientPoolManager evcacheClientPoolManager(final IConnectionBuilder connectionBuilder,
                                                             final EVCacheNodeList evcacheNodeList) {
        return new EVCacheClientPoolManager(connectionBuilder, evcacheNodeList);
    }
}
