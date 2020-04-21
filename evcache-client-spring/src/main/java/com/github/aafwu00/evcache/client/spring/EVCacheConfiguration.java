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
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link EVCacheManager}
 *
 * @author Taeho Kim
 */
public class EVCacheConfiguration {
    public static final String PATTERN_KEY_PREFIX = "[^:\\s]*";
    /**
     * Name of the Cache, {@link org.springframework.cache.annotation.Cacheable#cacheNames()}
     */
    private final String name;
    /**
     * Name of the EVCache App, Cluster Name, Recommend Upper Case. {@link com.netflix.evcache.EVCache.Builder#setAppName(String)}
     */
    private final String appName;
    /**
     * the minimum number of stripes (locks) required. effected only {@link EVCacheImpl#get(Object, java.util.concurrent.Callable)}, {@link EVCacheImpl#putIfAbsent(Object, Object)}
     */
    private final int striped;
    private final EVCacheClientPoolConfigurationProperties properties;

    /**
     * Instantiates a new EVCache configuration.
     *
     * @param name       Name of the Cache, {@link org.springframework.cache.annotation.Cacheable#cacheNames()}
     * @param striped    the minimum number of stripes (locks) required. effected only {@link EVCacheImpl#get(Object, java.util.concurrent.Callable)}, {@link EVCacheImpl#putIfAbsent(Object, Object)}
     * @param appName    Name of the EVCache App, Cluster Name, Recommend Upper Case
     * @param properties EVCache Client Configuration
     */
    public EVCacheConfiguration(final String name,
                                final int striped,
                                final String appName,
                                final EVCacheClientPoolConfigurationProperties properties) {
        Assert.state(StringUtils.isNotBlank(name), "`name` must not be blank");
        Assert.state(StringUtils.isNotBlank(appName), "`appName` must not be blank");
        Assert.notNull(properties, "`properties` must not be null");
        Assert.state(striped > 0, "`striped` must be positive value");
        this.name = name;
        this.striped = striped;
        this.appName = appName;
        this.properties = properties;
    }

    /**
     * Instantiates a new EVCache configuration.
     *
     * @param name                     Name of the Cache, {@link org.springframework.cache.annotation.Cacheable#cacheNames()}
     * @param striped                  the minimum number of stripes (locks) required. effected only {@link EVCacheImpl#get(Object, java.util.concurrent.Callable)}, {@link EVCacheImpl#putIfAbsent(Object, Object)}
     * @param appName                  Name of the EVCache App, Cluster Name, Recommend Upper Case. {@link com.netflix.evcache.EVCache.Builder#setAppName(String)}
     * @param keyPrefix                Name of Cache Prefix Key, Don't contain colon(:) and whitespace character. {@link com.netflix.evcache.EVCache.Builder#setCachePrefix(String)}
     * @param timeToLive               Default Time To Live(TTL), Seconds. {@link com.netflix.evcache.EVCache.Builder#setDefaultTTL(Duration)}
     * @param retryEnabled             Retry across Server Group for cache misses and exceptions. {@link com.netflix.evcache.EVCache.Builder#setRetry(boolean)}
     * @param exceptionThrowingEnabled Whether or not exception throwing is to be enabled. {@link com.netflix.evcache.EVCache.Builder#setExceptionThrowing(boolean)}
     */
    public EVCacheConfiguration(final String name,
                                final int striped,
                                final String appName,
                                final String keyPrefix,
                                final Duration timeToLive,
                                final boolean retryEnabled,
                                final boolean exceptionThrowingEnabled) {
        this(name, striped, appName, create(keyPrefix, timeToLive, retryEnabled, exceptionThrowingEnabled));
    }

    private static EVCacheClientPoolConfigurationProperties create(final String keyPrefix,
                                                                   final Duration timeToLive,
                                                                   final boolean retryEnabled,
                                                                   final boolean exceptionThrowingEnabled) {
        Assert.notNull(keyPrefix, "`keyPrefix` must not be null");
        Assert.state(keyPrefix.matches(PATTERN_KEY_PREFIX), "`keyPrefix` must not contain colon(:) or whitespace");
        Assert.notNull(timeToLive, "`timeToLive` must not be null");
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
        return Objects.equals(name, that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public String getName() {
        return name;
    }

    public int getStriped() {
        return striped;
    }

    public String getAppName() {
        return appName;
    }

    public EVCacheClientPoolConfigurationProperties getProperties() {
        return properties;
    }
}
