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
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import static com.github.aafwu00.evcache.server.spring.cloud.MemcachedHealthIndicator.DURATION;
import static com.github.aafwu00.evcache.server.spring.cloud.MemcachedHealthIndicator.EXPIRATION;
import static com.github.aafwu00.evcache.server.spring.cloud.MemcachedHealthIndicator.KEY;
import static com.github.aafwu00.evcache.server.spring.cloud.MemcachedHealthIndicator.VALUE;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Simple implementation of a {@link ReactiveHealthIndicator} returning status information for Memcached.
 *
 * @author Taeho Kim
 */
public class MemcachedReactiveHealthIndicator extends AbstractReactiveHealthIndicator {
    private final MemcachedClient client;

    /**
     * Default Constructor
     *
     * @param client evcache`s memcached client
     */
    public MemcachedReactiveHealthIndicator(final MemcachedClient client) {
        super();
        Assert.notNull(client, "`client` must not be null");
        this.client = client;
    }

    @Override
    protected Mono<Health> doHealthCheck(final Health.Builder builder) {
        return trySetOperation().zipWith(tryGetOperation(), this::isSuccess)
                                .map(success -> success ? builder.up().build() : builder.outOfService().build());
    }

    private Mono<Boolean> tryGetOperation() {
        return Mono.fromCallable(() -> client.asyncGet(KEY).get(DURATION, SECONDS))
                   .map(VALUE::equals);
    }

    private Mono<Boolean> trySetOperation() {
        return Mono.fromCallable(() -> client.set(KEY, EXPIRATION, VALUE).get(DURATION, SECONDS));
    }

    private Boolean isSuccess(final Boolean isSuccessSet, final Boolean isSuccessGet) {
        return isSuccessSet && isSuccessGet;
    }
}
