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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.netflix.evcache.EVCache.Call;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.evcache.metrics.Stats;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Taeho Kim
 */
class EVCacheMeterBinderTest {
    private EVCache cache;
    private EVCacheMeterBinder binder;
    private Stats stats;

    @BeforeEach
    void setUp() {
        cache = mock(EVCache.class);
        doReturn("app").when(cache).getAppName();
        doReturn("prefix").when(cache).getCachePrefix();
        binder = spy(new EVCacheMeterBinder(cache, "test", emptyList()));
        EVCacheMetricsFactory.getAllMetrics().clear();
        stats = EVCacheMetricsFactory.getStats("app", "prefix");
        call();
    }

    private void call() {
        stats.operationCompleted(Call.GET, 1);
        stats.operationCompleted(Call.GET_AND_TOUCH, 1);
        stats.operationCompleted(Call.SET, 1);
        stats.operationCompleted(Call.REPLACE, 1);
        stats.operationCompleted(Call.DELETE, 1);
        stats.operationCompleted(Call.BULK, 1);
        stats.operationCompleted(Call.APPEND_OR_ADD, 1);
        stats.operationCompleted(Call.ADD, 1);
        stats.operationCompleted(Call.APPEND, 1);
        stats.operationCompleted(Call.INCR, 1);
        stats.operationCompleted(Call.DECR, 1);
        stats.cacheHit(Call.GET);
        stats.cacheMiss(Call.GET);
        stats.cacheHit(Call.BULK);
        stats.cacheMiss(Call.BULK);
    }

    @AfterEach
    void tearDown() {
        EVCacheMetricsFactory.getAllMetrics().clear();
    }

    @Test
    void evictionCount() {
        assertThat(binder.evictionCount()).isNotNull();
    }

    @Test
    void hitCount() {
        assertThat(binder.hitCount()).isNotNull();
    }

    @Test
    void missCount() {
        assertThat(binder.missCount()).isNotNull();
    }

    @Test
    void putCount() {
        assertThat(binder.putCount()).isNotNull();
    }

    @Test
    void size_not_supported() {
        assertThat(binder.size()).isNull();
    }

    @Test
    void bindImplementationSpecificMetrics() {
        final MeterRegistry registry = spy(new SimpleMeterRegistry());
        binder.bindImplementationSpecificMetrics(registry);
        assertAll(
            () -> assertThat(registry.get("cache.get.calls")).isNotNull(),
            () -> assertThat(registry.get("cache.get.duration")).isNotNull(),
            () -> assertThat(registry.get("cache.bulk.calls")).isNotNull(),
            () -> assertThat(registry.get("cache.bulk.duration")).isNotNull(),
            () -> assertThat(registry.get("cache.bulk.hit")).isNotNull(),
            () -> assertThat(registry.get("cache.bulk.miss")).isNotNull()
        );
    }
}
