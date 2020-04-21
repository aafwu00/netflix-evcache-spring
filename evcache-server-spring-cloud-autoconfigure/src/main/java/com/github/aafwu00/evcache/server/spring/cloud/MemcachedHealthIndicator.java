/*
 * Copyright 2017-2020 the original author or authors.
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

import net.spy.memcached.MemcachedClient;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for Memcached.
 *
 * @author Taeho Kim
 */
public class MemcachedHealthIndicator extends AbstractHealthIndicator {
    protected static final String KEY = "__com.netflix.evcache.server.healthcheck";
    protected static final String VALUE = "Greed is good!";
    protected static final long DURATION = 5;
    protected static final int EXPIRATION = 300;
    private final MemcachedClient client;

    /**
     * Default Constructor
     *
     * @param client evcache`s memcached client
     */
    public MemcachedHealthIndicator(final MemcachedClient client) {
        super();
        Assert.notNull(client, "`client` must not be null");
        this.client = client;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        if (isAvailableSetOperation() && isValidValue()) {
            builder.up();
        } else {
            builder.outOfService();
        }
    }

    private boolean isValidValue() throws InterruptedException, TimeoutException, ExecutionException {
        return VALUE.equals(client.asyncGet(KEY).get(DURATION, SECONDS));
    }

    private Boolean isAvailableSetOperation() throws InterruptedException, ExecutionException, TimeoutException {
        return client.set(KEY, EXPIRATION, VALUE).get(DURATION, SECONDS);
    }
}
