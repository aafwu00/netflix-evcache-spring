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

package com.github.aafwu00.evcache.client.spring;

import com.netflix.evcache.EVCacheClientPoolConfigurationProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Configuration for {@link EVCacheManager}
 *
 * @author Taeho Kim
 */
public class EVCacheConfiguration {
    /**
     * Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheNames
     */
    private final String name;
    /**
     * Name of the EVCache App, Cluster Name, Recommend Upper Case
     */
    private final String appName;
    private final EVCacheClientPoolConfigurationProperties properties;

    /**
     * Instantiates a new EVCache configuration.
     *
     * @param name       Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheNames
     * @param appName    Name of the EVCache App, Cluster Name, Recommend Upper Case
     * @param properties EVCache Client Configuration
     */
    public EVCacheConfiguration(final String name,
                                final String appName,
                                final EVCacheClientPoolConfigurationProperties properties) {
        this.name = name;
        this.appName = appName;
        this.properties = requireNonNull(properties);
    }

    /**
     * Instantiates a new EVCache configuration.
     *
     * @param name                     Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheNames
     * @param appName                  Name of the EVCache App, Cluster Name, Recommend Upper Case
     * @param keyPrefix                Name of Cache Prefix Key, Don't contain colon(:) character
     * @param timeToLive               Default Time To Live(TTL), Seconds
     * @param retryEnabled             Retry across Server Group for cache misses and exceptions
     * @param exceptionThrowingEnabled Whether or not exception throwing is to be enabled.
     */
    public EVCacheConfiguration(final String name,
                                final String appName,
                                final String keyPrefix,
                                final Duration timeToLive,
                                final boolean retryEnabled,
                                final boolean exceptionThrowingEnabled) {
        this(name, appName, create(keyPrefix, timeToLive, retryEnabled, exceptionThrowingEnabled));
    }

    private static EVCacheClientPoolConfigurationProperties create(final String keyPrefix,
                                                                   final Duration timeToLive,
                                                                   final boolean retryEnabled,
                                                                   final boolean exceptionThrowingEnabled) {
        final EVCacheClientPoolConfigurationProperties result = new EVCacheClientPoolConfigurationProperties();
        result.setKeyPrefix(keyPrefix);
        result.setTimeToLive(timeToLive);
        result.setRetryEnabled(retryEnabled);
        result.setExceptionThrowingEnabled(exceptionThrowingEnabled);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EVCacheConfiguration that = (EVCacheConfiguration) obj;
        return new EqualsBuilder()
            .append(name, that.name)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(name)
            .toHashCode();
    }

    public String getName() {
        return name;
    }

    public String getAppName() {
        return appName;
    }

    public EVCacheClientPoolConfigurationProperties getProperties() {
        return properties;
    }
}
