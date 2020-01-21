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

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;

import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Sets.newHashSet;

public class EVCacheMeterBinder extends CacheMeterBinder {
    private final EVCacheMetricsFactory instance;
    private final Set<Tag> hitCountTags;
    private final Set<Tag> missCountTags;
    private final Set<Tag> putCountTags;
    private final Set<Tag> evictionCountTags;

    /**
     * Record metrics on a JCache cache.
     *
     * @param cache The cache to instrument.
     * @param tags  Tags to apply to all recorded metrics.Must be an even number of arguments representing key/value pairs of tags.
     */
    public EVCacheMeterBinder(final EVCache cache, final Iterable<io.micrometer.core.instrument.Tag> tags) {
        super(cache, cache.getName(), tags);
        instance = EVCacheMetricsFactory.getInstance();
        hitCountTags = newHashSet(Tag.of(EVCacheMetricsFactory.CACHE, cache.getAppName()),
                                  Tag.of(EVCacheMetricsFactory.PREFIX, cache.getKeyPrefix()),
                                  Tag.of(EVCacheMetricsFactory.CALL_TYPE_TAG, EVCacheMetricsFactory.READ),
                                  Tag.of(EVCacheMetricsFactory.CALL_TAG, EVCacheMetricsFactory.GET_OPERATION),
                                  Tag.of(EVCacheMetricsFactory.CACHE_HIT, EVCacheMetricsFactory.YES));
        missCountTags = newHashSet(Tag.of(EVCacheMetricsFactory.CACHE, cache.getAppName()),
                                   Tag.of(EVCacheMetricsFactory.PREFIX, cache.getKeyPrefix()),
                                   Tag.of(EVCacheMetricsFactory.CALL_TYPE_TAG, EVCacheMetricsFactory.READ),
                                   Tag.of(EVCacheMetricsFactory.CALL_TAG, EVCacheMetricsFactory.GET_OPERATION),
                                   Tag.of(EVCacheMetricsFactory.CACHE_HIT, EVCacheMetricsFactory.NO));
        putCountTags = newHashSet(Tag.of(EVCacheMetricsFactory.CACHE, cache.getAppName()),
                                  Tag.of(EVCacheMetricsFactory.PREFIX, cache.getKeyPrefix()),
                                  Tag.of(EVCacheMetricsFactory.CALL_TYPE_TAG, EVCacheMetricsFactory.WRITE),
                                  Tag.of(EVCacheMetricsFactory.CALL_TAG, EVCacheMetricsFactory.SET_OPERATION));
        evictionCountTags = newHashSet(Tag.of(EVCacheMetricsFactory.CACHE, cache.getAppName()),
                                       Tag.of(EVCacheMetricsFactory.PREFIX, cache.getKeyPrefix()),
                                       Tag.of(EVCacheMetricsFactory.CALL_TYPE_TAG, EVCacheMetricsFactory.WRITE),
                                       Tag.of(EVCacheMetricsFactory.CALL_TAG, EVCacheMetricsFactory.DELETE_OPERATION));
    }

    @Override
    protected Long size() {
        // EVCache statistics don't support size
        return null;
    }

    @Override
    protected long hitCount() {
        return sum(this::containHitCount);
    }

    private boolean containHitCount(final Timer timer) {
        return containAllTags(timer, hitCountTags);
    }

    private long sum(final Predicate<Timer> predicate) {
        return instance.getAllTimers()
                       .values()
                       .stream()
                       .filter(timer -> EVCacheMetricsFactory.OVERALL_CALL.equals(timer.id().name()))
                       .filter(predicate)
                       .mapToLong(Timer::count)
                       .sum();
    }

    private boolean containAllTags(final Timer timer, final Set<Tag> tags) {
        return tags.stream()
                   .allMatch(tag -> contains(timer.id().tags(), tag));
    }

    @Override
    protected Long missCount() {
        return sum(this::containMissCount);
    }

    private boolean containMissCount(final Timer timer) {
        return containAllTags(timer, missCountTags);
    }

    @Override
    protected Long evictionCount() {
        return sum(this::containEvictionCount);
    }

    private boolean containEvictionCount(final Timer timer) {
        return containAllTags(timer, evictionCountTags);
    }

    @Override
    protected long putCount() {
        return sum(this::containPutCount);
    }

    private boolean containPutCount(final Timer timer) {
        return containAllTags(timer, putCountTags);
    }

    @Override
    protected void bindImplementationSpecificMetrics(final MeterRegistry registry) {
        // nothing
    }
}
