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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netflix.appinfo.HealthCheckHandler;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;

import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicHostname;
import static com.netflix.appinfo.AmazonInfo.MetaDataKey.publicIpv4;
import static com.netflix.evcache.pool.DiscoveryNodeListProvider.DEFAULT_PORT;
import static java.util.Objects.requireNonNull;

/**
 * EVCache Server Health Configuration that setting up
 *
 * @author Taeho Kim
 */
@Configuration
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@ConditionalOnProperty(value = "evcache.server.health.enabled", matchIfMissing = true)
@AutoConfigureAfter(UtilAutoConfiguration.class)
@AutoConfigureBefore(EurekaClientAutoConfiguration.class)
public class EVCacheServerHealthAutoConfiguration {
    private final ConfigurableEnvironment environment;
    private final InetUtils inetUtils;

    public EVCacheServerHealthAutoConfiguration(final ConfigurableEnvironment environment, final InetUtils inetUtils) {
        this.environment = requireNonNull(environment);
        this.inetUtils = requireNonNull(inetUtils);
    }

    @Bean
    @ConditionalOnMissingBean(MemcachedClient.class)
    public MemcachedClientFactoryBean memcachedClient() {
        final MemcachedClientFactoryBean bean = new MemcachedClientFactoryBean();
        bean.setServers(address() + ":" + port());
        return bean;
    }

    private String address() {
        final InetUtils.HostInfo hostInfo = inetUtils.findFirstNonLoopbackHostInfo();
        return environment.getProperty("evcache." + publicHostname.getName(),
                                       environment.getProperty("evcache." + publicIpv4.getName(), hostInfo.getIpAddress()));
    }

    private String port() {
        return environment.getProperty("rend.port", environment.getProperty("evcache.port", DEFAULT_PORT));
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.eureka.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public HealthCheckHandler memcachedHealthCheckHandler(final MemcachedClient client) {
        return new MemcachedHealthCheckHandler(client);
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.memcached.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedHealthIndicator memcachedHealthIndicator(final MemcachedClient client) {
        return new MemcachedHealthIndicator(client);
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.metrics.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedMetrics memcachedMetrics(final MemcachedClient client) {
        return new MemcachedMetrics(client);
    }
}
