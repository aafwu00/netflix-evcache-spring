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

package com.github.aafwu00.evcache.client.spring.cloud;

import java.util.Properties;

import javax.inject.Provider;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import com.github.aafwu00.evcache.client.spring.boot.ConditionalOnEVCache;
import com.github.aafwu00.evcache.client.spring.boot.EVCacheAutoConfiguration;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.EVCache;
import com.netflix.evcache.connection.ConnectionFactoryProvider;
import com.netflix.evcache.connection.IConnectionFactoryProvider;
import com.netflix.evcache.pool.EVCacheClientPoolManager;

import static org.springframework.aop.support.AopUtils.isAopProxy;

/**
 * Spring configuration for configuring EVCache defaults to be Eureka based
 *
 * @author Taeho Kim
 * @see ArchaiusAutoConfiguration
 * @see EurekaClientAutoConfiguration
 * @see EVCacheAutoConfiguration
 */
@Configuration
@ConditionalOnEVCache
@ConditionalOnEVCacheCloud
@AutoConfigureAfter({ArchaiusAutoConfiguration.class, EurekaClientAutoConfiguration.class})
@AutoConfigureBefore(EVCacheAutoConfiguration.class)
public class EVCacheCloudAutoConfiguration {
    @Bean
    public HasFeatures evcacheCloudClientFeature() {
        return HasFeatures.namedFeature("Netflix EVCache Cloud Client", EVCache.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public IConnectionFactoryProvider connectionFactoryProvider() {
        return new ConnectionFactoryProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public EVCacheClientPoolManager evcacheClientPoolManager(final ConfigurableEnvironment environment,
                                                             final ApplicationInfoManager applicationInfoManager,
                                                             final EurekaClient eurekaClient,
                                                             final Provider<IConnectionFactoryProvider> connectionFactoryProvider) {
        appendProperty(environment);
        return new EVCacheClientPoolManager(applicationInfoManager, discoveryClient(eurekaClient), connectionFactoryProvider);
    }

    private void appendProperty(final ConfigurableEnvironment environment) {
        if (environment.containsProperty("evcache.use.simple.node.list.provider")) {
            return;
        }
        final Properties source = new Properties();
        source.setProperty("evcache.use.simple.node.list.provider", "false");
        environment.getPropertySources().addLast(new PropertiesPropertySource("evcacheSpringCloud", source));
    }

    private DiscoveryClient discoveryClient(final EurekaClient eurekaClient) {
        if (isAopProxy(eurekaClient)) {
            final TargetSource targetSource = Advised.class.cast(eurekaClient).getTargetSource();
            try {
                return DiscoveryClient.class.cast(targetSource.getTarget());
                // CHECKSTYLE:OFF
            } catch (final Exception e) {
                // CHECKSTYLE:ON
                throw new IllegalStateException(e);
            }
        }
        return DiscoveryClient.class.cast(eurekaClient);
    }
}
