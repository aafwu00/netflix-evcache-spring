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

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Spectator;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;

import static java.util.Objects.requireNonNull;

/**
 * A {@link MeterBinder} implementation that provides EVCache statistics.
 *
 * @author Taeho Kim
 */
public class EVCacheMeterBinder extends CacheMeterBinder {
    private static final String RESULT = "result";
    private final EVCache cache;
    private final com.netflix.evcache.metrics.EVCacheMetrics metrics;
    private final StepCounter deleteCall;

    public EVCacheMeterBinder(final EVCache cache, final String cacheName, final Iterable<Tag> tags) {
        super(cache, cacheName, Tags.concat(tags,
                                            "appName", cache.getAppName(),
                                            "cachePrefix", cache.getCachePrefix()));
        Spectator.globalRegistry().add(new DefaultRegistry());
        this.cache = requireNonNull(cache);
        this.metrics = com.netflix.evcache.metrics.EVCacheMetrics.class.cast(EVCacheMetricsFactory.getStats(cache.getAppName(),
                                                                                                            cache.getCachePrefix()));
        this.deleteCall = EVCacheMetricsFactory.getStepCounter(cache.getAppName(), cache.getCachePrefix(), "DeleteCall");
    }

    @Override
    protected Long size() {
        // EVCache statistics don't support size
        return null;
    }

    @Override
    protected long hitCount() {
        return metrics.getCacheHits();
    }

    @Override
    protected Long missCount() {
        return metrics.getCacheMiss();
    }

    @Override
    protected Long evictionCount() {
        return deleteCall.getValue().longValue();
    }

    @Override
    protected long putCount() {
        return metrics.getSetCalls();
    }

    @Override
    protected void bindImplementationSpecificMetrics(final MeterRegistry registry) {
        Gauge.builder("cache.get.calls", cache, c -> metrics.getGetCalls())
             .tags(getTagsWithCacheName()).tag(RESULT, "calls")
             .description("The number of hits (reads) of near cache entries owned by this member")
             .register(registry);
        Gauge.builder("cache.get.duration", cache, c -> metrics.getGetDuration())
             .tags(getTagsWithCacheName()).tag(RESULT, "duration")
             .description("The number of hits (reads) of near cache entries owned by this member")
             .register(registry);
        Gauge.builder("cache.bulk.calls", cache, c -> metrics.getBulkCalls())
             .tags(getTagsWithCacheName()).tag(RESULT, "calls")
             .description("the number of times cache lookup methods have returned an uncached (newly loaded) value, or null")
             .register(registry);
        Gauge.builder("cache.bulk.duration", cache, c -> metrics.getBulkDuration())
             .tags(getTagsWithCacheName()).tag(RESULT, "duration")
             .description("")
             .register(registry);
        Gauge.builder("cache.bulk.hit", cache, c -> metrics.getBulkHits())
             .tags(getTagsWithCacheName()).tag(RESULT, "hit")
             .description("The number of times cache lookup methods have returned a cached value.")
             .register(registry);
        Gauge.builder("cache.bulk.miss", cache, c -> metrics.getBulkMiss())
             .tags(getTagsWithCacheName()).tag(RESULT, "miss")
             .description("the number of times cache lookup methods have returned an uncached (newly loaded) value, or null")
             .register(registry);
    }
}
