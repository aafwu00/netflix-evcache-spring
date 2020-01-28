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
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Memcached Server Health Configuration that setting up
 *
 * @author Taeho Kim
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MemcachedClient.class)
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@ConditionalOnEnabledHealthIndicator("memcached")
@AutoConfigureAfter(MemcachedAutoConfiguration.class)
@SuppressWarnings({"checkstyle:linelength", "checkstyle:indentation"})
public class MemcachedHealthContributorAutoConfiguration
    extends CompositeHealthContributorConfiguration<MemcachedHealthIndicator, MemcachedClient> {
    @Bean
    @ConditionalOnMissingBean(name = {"memcachedHealthIndicator", "memcachedHealthContributor"})
    public HealthContributor memcachedHealthContributor(final Map<String, MemcachedClient> memcachedClients) {
        return createContributor(memcachedClients);
    }
}
