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

package com.github.aafwu00.evcache.server.spring.cloud;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import net.spy.memcached.MemcachedClient;

import java.util.Arrays;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

/**
 * A Eureka health checker for Memcached
 *
 * @author Taeho Kim
 */
public class MemcachedHealthCheckHandler implements HealthCheckHandler {
    private final MemcachedHealthIndicator healthIndicator;

    public MemcachedHealthCheckHandler(final MemcachedHealthIndicator healthIndicator) {
        this.healthIndicator = requireNonNull(healthIndicator);
    }

    public MemcachedHealthCheckHandler(final MemcachedClient client) {
        this(new MemcachedHealthIndicator(client));
    }

    @Override
    public InstanceInfo.InstanceStatus getStatus(final InstanceInfo.InstanceStatus instanceStatus) {
        return Arrays.stream(InstanceInfo.InstanceStatus.values())
                     .filter(this::sameStatusName)
                     .findFirst()
                     .orElse(UNKNOWN);
    }

    private boolean sameStatusName(final InstanceInfo.InstanceStatus status) {
        return equalsIgnoreCase(status.name(), healthIndicator.health().getStatus().getCode());
    }
}
