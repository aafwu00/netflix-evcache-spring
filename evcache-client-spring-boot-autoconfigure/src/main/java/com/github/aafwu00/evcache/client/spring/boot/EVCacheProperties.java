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
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Configuration properties for the EVCache.
 *
 * @author Taeho Kim
 */
@Validated
@ConfigurationProperties("evcache")
public class EVCacheProperties {
    /**
     * Enable EVCache
     */
    private boolean enabled = true;
    /**
     * Clusters
     */
    @NotEmpty
    @Valid
    @NestedConfigurationProperty
    private List<Cluster> clusters;

    protected Set<EVCacheConfiguration> toConfigurations() {
        return clusters.stream()
                       .map(Cluster::toConfiguration)
                       .collect(toSet());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(final List<Cluster> clusters) {
        this.clusters = clusters;
    }

    @Validated
    @ConfigurationProperties("evcache.clusters[]")
    public static class Cluster {
        private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofSeconds(900);
        /**
         * Name of the Cache, @{@link org.springframework.cache.annotation.Cacheable} cacheName,
         * Same as `appName` + `.` + `keyPrefix` when name is blank
         */
        private String name;
        /**
         * Name of the EVCache App cluster, Recommend Upper Case
         */
        @NotBlank
        private String appName;
        /**
         * Key Prefix, Don't contain colon(:) character
         * same as {@link org.springframework.cache.annotation.Cacheable} cacheNames
         */
        @Pattern(regexp = "[^:]*$")
        private String keyPrefix = "";
        /**
         * Default Time To Live(TTL)
         */
        private Duration timeToLive = DEFAULT_TIME_TO_LIVE;
        /**
         * Retry across Server Group for cache misses and exceptions
         */
        private boolean retryEnabled = true;
        /**
         * Whether or not exception throwing is to be enabled.
         */
        private boolean exceptionThrowingEnabled;

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

        public void setName(final String name) {
            this.name = name;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(final String appName) {
            this.appName = appName;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(final String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(final Duration timeToLive) {
            this.timeToLive = timeToLive;
        }

        public boolean isRetryEnabled() {
            return retryEnabled;
        }

        public void setRetryEnabled(final boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
        }

        public boolean isExceptionThrowingEnabled() {
            return exceptionThrowingEnabled;
        }

        public void setExceptionThrowingEnabled(final boolean exceptionThrowingEnabled) {
            this.exceptionThrowingEnabled = exceptionThrowingEnabled;
        }
    }
}

