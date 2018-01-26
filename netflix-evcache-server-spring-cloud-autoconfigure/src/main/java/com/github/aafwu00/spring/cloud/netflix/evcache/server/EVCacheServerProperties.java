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

package com.github.aafwu00.spring.cloud.netflix.evcache.server;

import javax.validation.constraints.Min;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Configuration properties for the EVCache Server.
 *
 * @author Taeho Kim
 */
@Validated
@ConfigurationProperties(prefix = "evcache.server")
public class EVCacheServerProperties {
    private static final int DEFAULT_PORT = 11211;
    /**
     * Enable EVCache Server
     */
    private boolean enabled = true;
    /**
     * Evcache Server Group Name, Name of Replica Set, Shard Name
     */
    @NotBlank
    private String group = "Default";
    /**
     * Hostname of Memcached Or Rend, If blank using first non loopback hostname
     */
    private String hostname;
    /**
     * Default Port of Memcached Or Rend
     */
    @Min(0)
    private int port = DEFAULT_PORT;

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EVCacheServerProperties that = (EVCacheServerProperties) obj;
        return new EqualsBuilder()
            .append(enabled, that.enabled)
            .append(hostname, that.hostname)
            .append(port, that.port)
            .append(group, that.group)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(enabled)
            .append(hostname)
            .append(port)
            .append(group)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
            .append("enabled", enabled)
            .append("hostname", hostname)
            .append("port", port)
            .append("group", group)
            .toString();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }
}

