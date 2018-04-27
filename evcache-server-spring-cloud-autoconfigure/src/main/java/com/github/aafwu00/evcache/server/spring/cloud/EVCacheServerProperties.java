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

package com.github.aafwu00.evcache.server.spring.cloud;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the EVCar.
 *
 * @author Taeho Kim
 */
@Validated
@ConfigurationProperties(prefix = "evcar")
public class EVCacheServerProperties {
    private static final int DEFAULT_PORT = 11211;
    /**
     * Enable EVCar
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

