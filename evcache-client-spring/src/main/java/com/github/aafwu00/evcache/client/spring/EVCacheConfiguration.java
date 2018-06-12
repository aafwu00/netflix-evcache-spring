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

package com.github.aafwu00.evcache.client.spring;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static org.apache.commons.lang3.Validate.inclusiveBetween;
import static org.apache.commons.lang3.Validate.matchesPattern;
import static org.apache.commons.lang3.Validate.notEmpty;

/**
 * Configuration for {@link EVCacheManager}
 *
 * @author Taeho Kim
 */
public class EVCacheConfiguration {
    /**
     * Name of the EVCache App, Cluster Name, Recommend Upper Case
     */
    private final String appName;
    /**
     * Cache Prefix Key, Don't contain colon(:) character
     */
    private final String cachePrefix;
    /**
     * Default Time To Live(TTL), Seconds
     */
    private final int timeToLive;
    /**
     * Whether to allow for {@code null} values
     */
    private final boolean allowNullValues;
    /**
     * Retry across Server Group for cache misses and exceptions
     */
    private final boolean serverGroupRetry;
    /**
     * Exceptions are not propagated and null values are returned
     */
    private final boolean enableExceptionThrowing;

    /**
     * Instantiates a new EVCache configuration.
     *
     * @param appName                 the evcache app name
     * @param cachePrefix             the cache prefix
     * @param timeToLive              the time to live
     * @param allowNullValues         the allow null values
     * @param serverGroupRetry        the server group retry
     * @param enableExceptionThrowing the enable exception throwing
     */
    public EVCacheConfiguration(final String appName,
                                final String cachePrefix,
                                final int timeToLive,
                                final boolean allowNullValues,
                                final boolean serverGroupRetry,
                                final boolean enableExceptionThrowing) {
        this.appName = notEmpty(appName);
        this.cachePrefix = notEmpty(cachePrefix);
        this.timeToLive = timeToLive;
        this.allowNullValues = allowNullValues;
        this.serverGroupRetry = serverGroupRetry;
        this.enableExceptionThrowing = enableExceptionThrowing;
        matchesPattern(cachePrefix, "[^:]*$", "'cachePrefix' must not contain colon(:) character");
        inclusiveBetween(1, Integer.MAX_VALUE, timeToLive, "'timeToLive' must be positive integer");
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
            .append(appName, that.appName)
            .append(cachePrefix, that.cachePrefix)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(appName)
            .append(cachePrefix)
            .toHashCode();
    }

    public String getAppName() {
        return appName;
    }

    public String getCachePrefix() {
        return cachePrefix;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public boolean isServerGroupRetry() {
        return serverGroupRetry;
    }

    public boolean isEnableExceptionThrowing() {
        return enableExceptionThrowing;
    }
}
