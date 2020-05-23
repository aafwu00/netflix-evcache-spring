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

package com.github.aafwu00.evcache.client.spring.boot;

import com.github.aafwu00.evcache.client.spring.EVCacheConfiguration;
import com.github.aafwu00.evcache.client.spring.EVCacheImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static com.github.aafwu00.evcache.client.spring.EVCacheConfiguration.PATTERN_KEY_PREFIX;
import static java.util.stream.Collectors.toSet;

/**
 * Configuration properties for the EVCache.
 *
 * @author Taeho Kim
 */
@Validated
@ConstructorBinding
@ConfigurationProperties("evcache")
public class EVCacheProperties {
    /**
     * Enable EVCache
     */
    private final boolean enabled;
    /**
     * Whether to allow for {@code null} values
     */
    private final boolean allowNullValues;
    /**
     * Key is Name of the Cache {@link org.springframework.cache.annotation.Cacheable#cacheNames()},
     * Value is Clusters properties
     */
    @NotEmpty
    @Valid
    @NestedConfigurationProperty
    private final Map<String, Cluster> clusters;

    /**
     * @param enabled         Enable EVCache
     * @param allowNullValues whether to allow for {@code null} values
     * @param clusters        Key is Name of the Cache {@link org.springframework.cache.annotation.Cacheable#cacheNames()},
     *                        Value is Clusters properties
     */
    public EVCacheProperties(@DefaultValue("true") final boolean enabled,
                             @DefaultValue("true") final boolean allowNullValues,
                             @NotEmpty @Valid final Map<String, Cluster> clusters) {
        Assert.notEmpty(clusters, "`clusters` must not be empty");
        this.enabled = enabled;
        this.allowNullValues = allowNullValues;
        this.clusters = clusters;
    }

    protected Set<EVCacheConfiguration> toConfigurations() {
        return clusters.entrySet()
                       .stream()
                       .map(entry -> entry.getValue().toConfiguration(entry.getKey()))
                       .collect(toSet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAllowNullValues() {
        return allowNullValues;
    }

    public Map<String, Cluster> getClusters() {
        return clusters;
    }

    @Validated
    public static class Cluster {
        /**
         * the minimum number of stripes (locks) required. effected only {@link EVCacheImpl#get(Object, java.util.concurrent.Callable)}, {@link EVCacheImpl#putIfAbsent(Object, Object)}
         */
        private final int striped;
        /**
         * Name of the EVCache App cluster, Recommend Upper Case. {@link com.netflix.evcache.EVCache.Builder#setAppName(String)}
         */
        @NotBlank
        @NotNull
        private final String appName;
        /**
         * Key Prefix, Don't contain colon(:) and whitespace character. {@link com.netflix.evcache.EVCache.Builder#setCachePrefix(String)}
         */
        @NotNull
        @Pattern(regexp = PATTERN_KEY_PREFIX)
        private final String keyPrefix;
        /**
         * Default Time To Live(TTL). {@link com.netflix.evcache.EVCache.Builder#setDefaultTTL(Duration)}
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private final Duration timeToLive;
        /**
         * Retry across Server Group for cache misses and exceptions. {@link com.netflix.evcache.EVCache.Builder#setRetry(boolean)}
         */
        private final boolean retryEnabled;
        /**
         * Whether or not exception throwing is to be enabled. {@link com.netflix.evcache.EVCache.Builder#setExceptionThrowing(boolean)}
         */
        private final boolean exceptionThrowingEnabled;

        /**
         * @param appName                  Name of the EVCache App cluster, Recommend Upper Case. {@link com.netflix.evcache.EVCache.Builder#setAppName(String)}
         * @param keyPrefix                Key Prefix, Don't contain colon(:) and whitespace character. {@link com.netflix.evcache.EVCache.Builder#setCachePrefix(String)}
         * @param timeToLive               Default Time To Live(TTL). {@link com.netflix.evcache.EVCache.Builder#setDefaultTTL(Duration)}
         * @param retryEnabled             Retry across Server Group for cache misses and exceptions. {@link com.netflix.evcache.EVCache.Builder#setRetry(boolean)}
         * @param exceptionThrowingEnabled Whether or not exception throwing is to be enabled. {@link com.netflix.evcache.EVCache.Builder#setExceptionThrowing(boolean)}
         * @param striped                  the minimum number of stripes (locks) required.
         *                                 effected only {@link EVCacheImpl#get(Object, java.util.concurrent.Callable)}, {@link EVCacheImpl#putIfAbsent(Object, Object)}.
         *                                 default is zero, if zero then striped will be `{@link Runtime#availableProcessors()} * 4`.
         */
        public Cluster(@NotBlank @NotNull final String appName,
                       @Pattern(regexp = PATTERN_KEY_PREFIX) @DefaultValue("") final String keyPrefix,
                       @DefaultValue("900s") @NotNull final Duration timeToLive,
                       @DefaultValue("true") final boolean retryEnabled,
                       @DefaultValue("false") final boolean exceptionThrowingEnabled,
                       @DefaultValue("0") final int striped) {
            Assert.state(StringUtils.isNotBlank(appName), "`appName` must not be blank");
            Assert.notNull(keyPrefix, "`keyPrefix` must not be null");
            Assert.state(keyPrefix.matches(PATTERN_KEY_PREFIX), "`keyPrefix` must not contain colon(:) or whitespace");
            Assert.notNull(timeToLive, "`timeToLive` must not be null");
            Assert.state(striped >= 0, "`striped` must not be negative");
            this.appName = appName;
            this.keyPrefix = keyPrefix;
            this.timeToLive = timeToLive;
            this.retryEnabled = retryEnabled;
            this.exceptionThrowingEnabled = exceptionThrowingEnabled;
            this.striped = striped;
        }

        protected EVCacheConfiguration toConfiguration(final String name) {
            return new EVCacheConfiguration(name,
                                            determineStriped(),
                                            getAppName(),
                                            getKeyPrefix(),
                                            getTimeToLive(),
                                            isRetryEnabled(),
                                            isExceptionThrowingEnabled());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Cluster cluster = (Cluster) obj;
            return new EqualsBuilder().append(getAppName(), cluster.getAppName())
                                      .append(getKeyPrefix(), cluster.getKeyPrefix())
                                      .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(getAppName())
                                        .append(getKeyPrefix())
                                        .toHashCode();
        }

        protected int determineStriped() {
            return striped > 0 ? striped : Runtime.getRuntime().availableProcessors() * 4;
        }

        public int getStriped() {
            return striped;
        }

        public String getAppName() {
            return appName;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public Duration getTimeToLive() {
            return timeToLive;
        }

        public boolean isRetryEnabled() {
            return retryEnabled;
        }

        public boolean isExceptionThrowingEnabled() {
            return exceptionThrowingEnabled;
        }
    }
}

