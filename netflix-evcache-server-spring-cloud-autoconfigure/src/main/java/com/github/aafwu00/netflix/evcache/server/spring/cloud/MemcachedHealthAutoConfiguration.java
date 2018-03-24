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

package com.github.aafwu00.netflix.evcache.server.spring.cloud;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.HealthCheckHandler;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * EVCache Server Health Configuration that setting up
 *
 * @author Taeho Kim
 */
@Configuration
@ConditionalOnProperty(value = "evcache.server.enabled", matchIfMissing = true)
@AutoConfigureAfter(UtilAutoConfiguration.class)
@AutoConfigureBefore(EurekaClientAutoConfiguration.class)
@EnableConfigurationProperties(EVCacheServerProperties.class)
public class MemcachedHealthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MemcachedClient.class)
    public MemcachedClientFactoryBean memcachedClient(final InetUtils inetUtils, final EVCacheServerProperties properties) {
        final MemcachedClientFactoryBean bean = new MemcachedClientFactoryBean();
        final String hostname = defaultIfBlank(properties.getHostname(), inetUtils.findFirstNonLoopbackHostInfo().getIpAddress());
        bean.setServers(hostname + ":" + properties.getPort());
        return bean;
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.eureka.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public HealthCheckHandler healthCheckHandler(final MemcachedClient client) {
        return new MemcachedHealthCheckHandler(client);
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.metrics.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedMetrics memcachedMetrics(final MemcachedClient client) {
        return new MemcachedMetrics(client);
    }

    @Bean
    @ConditionalOnProperty(value = "evcache.server.health.memcached.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MemcachedHealthIndicator memcachedHealthIndicator(final MemcachedClient client) {
        return new MemcachedHealthIndicator(client);
    }
}
