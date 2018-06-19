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

package com.github.aafwu00.evcache.client.spring.boot;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import com.github.aafwu00.evcache.client.spring.EVCacheConfiguration;

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
        private static final int DEFAULT_TIME_TO_LIVE = 900;
        /**
         * Name of the Cache, @Cacheable cacheName, Same as `appName` + `.` + `cachePrefix` when name is blank
         */
        private String name;
        /**
         * Name of the EVCache App cluster, Recommend Upper Case
         */
        @NotBlank
        private String appName;
        /**
         * Cache Prefix Key, Don't contain colon(:) character
         * same as {@link org.springframework.cache.annotation.Cacheable} cacheNames
         */
        @NotBlank
        @Pattern(regexp = "[^:]+$")
        private String cachePrefix;
        /**
         * Default Time To Live(TTL), Seconds
         */
        @Min(0)
        private int timeToLive = DEFAULT_TIME_TO_LIVE;
        /**
         * Whether to allow for {@code null} values
         */
        private boolean allowNullValues = true;
        /**
         * Retry across Server Group for cache misses and exceptions
         */
        private boolean serverGroupRetry = true;
        /**
         * Exceptions are not propagated and null values are returned
         */
        private boolean enableExceptionThrowing;

        protected EVCacheConfiguration toConfiguration() {
            return new EVCacheConfiguration(determineName(),
                                            appName,
                                            cachePrefix,
                                            timeToLive,
                                            allowNullValues,
                                            serverGroupRetry,
                                            enableExceptionThrowing);
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

        public String determineName() {
            if (StringUtils.isBlank(name)) {
                return appName + "." + cachePrefix;
            }
            return name;
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

        public String getCachePrefix() {
            return cachePrefix;
        }

        public void setCachePrefix(final String cachePrefix) {
            this.cachePrefix = cachePrefix;
        }

        public int getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(final int timeToLive) {
            this.timeToLive = timeToLive;
        }

        public boolean isAllowNullValues() {
            return allowNullValues;
        }

        public void setAllowNullValues(final boolean allowNullValues) {
            this.allowNullValues = allowNullValues;
        }

        public boolean isServerGroupRetry() {
            return serverGroupRetry;
        }

        public void setServerGroupRetry(final boolean serverGroupRetry) {
            this.serverGroupRetry = serverGroupRetry;
        }

        public boolean isEnableExceptionThrowing() {
            return enableExceptionThrowing;
        }

        public void setEnableExceptionThrowing(final boolean enableExceptionThrowing) {
            this.enableExceptionThrowing = enableExceptionThrowing;
        }
    }
}

