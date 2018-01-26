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

package com.github.aafwu00.spring.netflix.evcache.client;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.inclusiveBetween;
import static org.apache.commons.lang3.Validate.matchesPattern;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Configuration for {@link EVCacheManager}
 *
 * @author Taeho Kim
 */
public class EVCacheConfiguration {
    /**
     * Cache name, Cache Prefix Key, Don't contain colon(:) character
     */
    private final String name;
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
     * Instantiates a new Ev cache configuration.
     *
     * @param name                    the name
     * @param timeToLive              the time to live
     * @param allowNullValues         the allow null values
     * @param serverGroupRetry        the server group retry
     * @param enableExceptionThrowing the enable exception throwing
     */
    public EVCacheConfiguration(final String name,
                                final int timeToLive,
                                final boolean allowNullValues,
                                final boolean serverGroupRetry,
                                final boolean enableExceptionThrowing) {
        this.name = notEmpty(name);
        this.timeToLive = timeToLive;
        this.allowNullValues = allowNullValues;
        this.serverGroupRetry = serverGroupRetry;
        this.enableExceptionThrowing = enableExceptionThrowing;
        matchesPattern(name, "[^:]*$", "'name' must not contain colon(:) character");
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
            .append(timeToLive, that.timeToLive)
            .append(allowNullValues, that.allowNullValues)
            .append(serverGroupRetry, that.serverGroupRetry)
            .append(enableExceptionThrowing, that.enableExceptionThrowing)
            .append(name, that.name)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(name)
            .append(timeToLive)
            .append(allowNullValues)
            .append(serverGroupRetry)
            .append(enableExceptionThrowing)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
            .append("name", name)
            .append("timeToLive", timeToLive)
            .append("allowNullValues", allowNullValues)
            .append("serverGroupRetry", serverGroupRetry)
            .append("enableExceptionThrowing", enableExceptionThrowing)
            .toString();
    }

    public String getName() {
        return name;
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
