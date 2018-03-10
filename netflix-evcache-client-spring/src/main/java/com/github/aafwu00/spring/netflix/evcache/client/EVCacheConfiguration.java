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
     * Whether to convert key to hashing
     */
    private final boolean keyHash;
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
     * @param keyHash            whether to convert key to digest
     * @param serverGroupRetry        the server group retry
     * @param enableExceptionThrowing the enable exception throwing
     */
    public EVCacheConfiguration(final String name,
                                final int timeToLive,
                                final boolean allowNullValues,
                                final boolean keyHash, final boolean serverGroupRetry,
                                final boolean enableExceptionThrowing) {
        this.name = notEmpty(name);
        this.timeToLive = timeToLive;
        this.allowNullValues = allowNullValues;
        this.keyHash = keyHash;
        this.serverGroupRetry = serverGroupRetry;
        this.enableExceptionThrowing = enableExceptionThrowing;
        matchesPattern(name, "[^:]*$", "'name' must not contain colon(:) character");
        inclusiveBetween(1, Integer.MAX_VALUE, timeToLive, "'timeToLive' must be positive integer");
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

    public boolean isKeyHash() {
        return keyHash;
    }

    public boolean isServerGroupRetry() {
        return serverGroupRetry;
    }

    public boolean isEnableExceptionThrowing() {
        return enableExceptionThrowing;
    }
}
