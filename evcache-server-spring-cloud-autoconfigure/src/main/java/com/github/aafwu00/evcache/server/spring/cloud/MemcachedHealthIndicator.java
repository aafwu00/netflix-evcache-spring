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

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import net.spy.memcached.MemcachedClient;

import static java.util.Objects.requireNonNull;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for Memcached.
 *
 * @author Taeho Kim
 */
public class MemcachedHealthIndicator extends AbstractHealthIndicator {
    private static final String KEY = "__com.netflix.evcache.server.healthcheck";
    private static final String VALUE = "Greed is good!";
    private static final long DURATION = 5;
    private static final int EXPIRATION = 300;
    private final MemcachedClient client;

    public MemcachedHealthIndicator(final MemcachedClient client) {
        super();
        this.client = requireNonNull(client);
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        if (client.set(KEY, EXPIRATION, VALUE).get()
            && VALUE.equals(client.asyncGet(KEY).get(DURATION, TimeUnit.SECONDS))) {
            builder.up();
        }
    }
}
