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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
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
     * Clusters properties
     */
    @NotEmpty
    @Valid
    @NestedConfigurationProperty
    private final List<Cluster> clusters;

    /**
     * @param enabled  Enable EVCache
     * @param clusters Clusters properties
     */
    public EVCacheProperties(@DefaultValue("true") final boolean enabled,
                             @NotEmpty @Valid final List<Cluster> clusters) {
        this.enabled = enabled;
        this.clusters = requireNonNull(clusters);
    }

    protected Set<EVCacheConfiguration> toConfigurations() {
        return clusters.stream()
                       .map(Cluster::toConfiguration)
                       .collect(toSet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    @Validated
    public static class Cluster {
        /**
         * Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheName,
         * Same as `appName` + `.` + `keyPrefix` when name is blank
         */
        private final String name;
        /**
         * Name of the EVCache App cluster, Recommend Upper Case
         */
        @NotBlank
        private final String appName;
        /**
         * Key Prefix, Don't contain colon(:) character
         */
        @Pattern(regexp = "[^:]*$")
        private final String keyPrefix;
        /**
         * Default Time To Live(TTL)
         */
        @DurationUnit(ChronoUnit.SECONDS)
        private final Duration timeToLive;
        /**
         * Retry across Server Group for cache misses and exceptions
         */
        private final boolean retryEnabled;
        /**
         * Whether or not exception throwing is to be enabled.
         */
        private final boolean exceptionThrowingEnabled;

        /**
         * @param name                     Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheName,  Same as
         *                                 `appName` + `.` + `keyPrefix` when name is blank
         * @param appName                  Name of the EVCache App cluster, Recommend Upper Case
         * @param keyPrefix                Key Prefix, Don't contain colon(:) character
         * @param timeToLive               Default Time To Live(TTL)
         * @param retryEnabled             Retry across Server Group for cache misses and exceptions
         * @param exceptionThrowingEnabled Whether or not exception throwing is to be enabled.
         */
        public Cluster(final String name,
                       @NotBlank final String appName,
                       @Pattern(regexp = "[^:]*$") @DefaultValue("") final String keyPrefix,
                       @DefaultValue("900s") final Duration timeToLive,
                       @DefaultValue("true") final boolean retryEnabled,
                       final boolean exceptionThrowingEnabled) {
            this.name = name;
            this.appName = requireNonNull(appName);
            this.keyPrefix = requireNonNull(keyPrefix);
            this.timeToLive = requireNonNull(timeToLive);
            this.retryEnabled = retryEnabled;
            this.exceptionThrowingEnabled = exceptionThrowingEnabled;
        }

        protected EVCacheConfiguration toConfiguration() {
            return new EVCacheConfiguration(determineName(),
                                            appName,
                                            keyPrefix,
                                            timeToLive,
                                            retryEnabled,
                                            exceptionThrowingEnabled);
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
            return new EqualsBuilder()
                .append(determineName(), cluster.determineName())
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(determineName())
                .toHashCode();
        }

        protected String determineName() {
            if (StringUtils.isBlank(getName())) {
                return getAppName() + "." + getKeyPrefix();
            }
            return getName();
        }

        public String getName() {
            return name;
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

