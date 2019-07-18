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

import java.util.stream.Stream;

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.google.common.collect.Iterables;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Timer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;

import static com.netflix.evcache.metrics.EVCacheMetricsFactory.CACHE;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.CACHE_HIT;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.CALL_TAG;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.CALL_TYPE_TAG;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.DELETE_OPERATION;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.GET_OPERATION;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.NO;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.OVERALL_CALL;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.PREFIX;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.READ;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.SET_OPERATION;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.WRITE;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.YES;

public class EVCacheMeterBinder extends CacheMeterBinder {
    private final EVCacheMetricsFactory instance;
    private final String appName;
    private final String keyPrefix;

    public EVCacheMeterBinder(final EVCache cache, final Iterable<Tag> tags) {
        super(cache, cache.getName(), tags);
        appName = cache.getAppName();
        keyPrefix = cache.getKeyPrefix();
        instance = EVCacheMetricsFactory.getInstance();
    }

    @Override
    protected Long size() {
        // EVCache statistics don't support size
        return null;
    }

    @Override
    protected long hitCount() {
        return timers().filter(timer -> containTag(timer.id(), CALL_TYPE_TAG, READ))
                       .filter(timer -> containTag(timer.id(), CALL_TAG, GET_OPERATION))
                       .filter(timer -> containTag(timer.id(), CACHE_HIT, YES))
                       .mapToLong(Timer::count)
                       .sum();
    }

    private Stream<Timer> timers() {
        return instance.getAllTimers()
                       .values()
                       .stream()
                       .filter(timer -> OVERALL_CALL.equals(timer.id().name()))
                       .filter(timer -> containTag(timer.id(), CACHE, appName))
                       .filter(timer -> containTag(timer.id(), PREFIX, keyPrefix));
    }

    private boolean containTag(final Id id, final String key, final String value) {
        return containTag(id, com.netflix.spectator.api.Tag.of(key, value));
    }

    private boolean containTag(final Id id, final com.netflix.spectator.api.Tag tag) {
        return contain(id.tags(), tag);
    }

    private boolean contain(final Iterable<com.netflix.spectator.api.Tag> tags, final com.netflix.spectator.api.Tag tag) {
        return Iterables.contains(tags, tag);
    }

    @Override
    protected Long missCount() {
        return timers().filter(timer -> containTag(timer.id(), CALL_TYPE_TAG, READ))
                       .filter(timer -> containTag(timer.id(), CALL_TAG, GET_OPERATION))
                       .filter(timer -> containTag(timer.id(), CACHE_HIT, NO))
                       .mapToLong(Timer::count)
                       .sum();
    }

    @Override
    protected Long evictionCount() {
        return timers().filter(timer -> containTag(timer.id(), CALL_TYPE_TAG, WRITE))
                       .filter(timer -> containTag(timer.id(), CALL_TAG, DELETE_OPERATION))
                       .mapToLong(Timer::count)
                       .sum();
    }

    @Override
    protected long putCount() {
        return timers().filter(timer -> containTag(timer.id(), CALL_TYPE_TAG, WRITE))
                       .filter(timer -> containTag(timer.id(), CALL_TAG, SET_OPERATION))
                       .mapToLong(Timer::count)
                       .sum();
    }

    @Override
    protected void bindImplementationSpecificMetrics(final MeterRegistry registry) {
    }
}
