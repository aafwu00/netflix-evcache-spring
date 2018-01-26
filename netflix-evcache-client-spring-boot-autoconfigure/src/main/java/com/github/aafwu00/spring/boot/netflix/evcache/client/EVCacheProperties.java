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

package com.github.aafwu00.spring.boot.netflix.evcache.client;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import com.github.aafwu00.spring.netflix.evcache.client.EVCacheConfiguration;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Configuration properties for the EVCache.
 *
 * @author Taeho Kim
 */
@Validated
@ConfigurationProperties(prefix = "evcache")
public class EVCacheProperties {
    /**
     * Enable EVCache
     */
    private boolean enabled = true;
    /**
     * Name of the EVCache App cluster, Recommend Upper Case
     */
    @NotBlank
    private String name;
    /**
     * The Prefixes.
     */
    @NotEmpty
    @Valid
    @NestedConfigurationProperty
    private List<Prefix> prefixes;

    protected List<EVCacheConfiguration> toConfigurations() {
        return prefixes.stream()
                       .map(Prefix::toConfiguration)
                       .collect(toList());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EVCacheProperties that = (EVCacheProperties) obj;
        return new EqualsBuilder()
            .append(enabled, that.enabled)
            .append(name, that.name)
            .append(prefixes, that.prefixes)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(enabled)
            .append(name)
            .append(prefixes)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
            .append("enabled", enabled)
            .append("name", name)
            .append("prefixes", prefixes)
            .toString();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Prefix> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(final List<Prefix> prefixes) {
        this.prefixes = prefixes;
    }

    @Validated
    public static class Prefix {
        private static final int DEFAULT_TIME_TO_LIVE = 900;
        /**
         * Cache name , Cache Prefix Key, Don't contain colon(:) character
         * same as {@link org.springframework.cache.annotation.Cacheable} cacheNames
         */
        @NotBlank
        @Pattern(regexp = "[^:]+$")
        private String name;
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
            return new EVCacheConfiguration(name, timeToLive, allowNullValues, serverGroupRetry, enableExceptionThrowing);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Prefix that = (Prefix) obj;
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
                .append("allowNullValues", timeToLive)
                .append("serverGroupRetry", serverGroupRetry)
                .append("enableExceptionThrowing", enableExceptionThrowing)
                .toString();
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
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

