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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.Timer;

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
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class EVCacheMeterBinderTest {
    private static final String CACHE_NAME = "name";
    private static final String APP_NAME = "appName";
    private static final String KEY_PREFIX = "keyPrefix";
    private final EVCacheMetricsFactory instance = EVCacheMetricsFactory.getInstance();
    private EVCacheMeterBinder binder;

    @BeforeEach
    void setUp() {
        Spectator.globalRegistry().add(new DefaultRegistry());
        final EVCache cache = mock(EVCache.class);
        doReturn(CACHE_NAME).when(cache).getName();
        doReturn(APP_NAME).when(cache).getAppName();
        doReturn(KEY_PREFIX).when(cache).getKeyPrefix();
        binder = new EVCacheMeterBinder(cache, emptySet());
        tripleCalled(timer(hitTags()));
        tripleCalled(timer(missTags()));
        tripleCalled(timer(evictionTags()));
        tripleCalled(timer(putTags()));
    }

    private void tripleCalled(final Timer timer) {
        timer.record(1, TimeUnit.MICROSECONDS);
        timer.record(1, TimeUnit.MICROSECONDS);
        timer.record(1, TimeUnit.MICROSECONDS);
    }

    private Timer timer(final List<Tag> tags) {
        return instance.getPercentileTimer(OVERALL_CALL, tags, Duration.ofMillis(100));
    }

    private List<Tag> hitTags() {
        return Arrays.asList(Tag.of(CACHE, APP_NAME),
                             Tag.of(PREFIX, KEY_PREFIX),
                             Tag.of(CALL_TYPE_TAG, READ),
                             Tag.of(CALL_TAG, GET_OPERATION),
                             Tag.of(CACHE_HIT, YES));
    }

    private List<Tag> missTags() {
        return Arrays.asList(Tag.of(CACHE, APP_NAME),
                             Tag.of(PREFIX, KEY_PREFIX),
                             Tag.of(CALL_TYPE_TAG, READ),
                             Tag.of(CALL_TAG, GET_OPERATION),
                             Tag.of(CACHE_HIT, NO));
    }

    private List<Tag> evictionTags() {
        return Arrays.asList(Tag.of(CACHE, APP_NAME),
                             Tag.of(PREFIX, KEY_PREFIX),
                             Tag.of(CALL_TYPE_TAG, WRITE),
                             Tag.of(CALL_TAG, DELETE_OPERATION));
    }

    private List<Tag> putTags() {
        return Arrays.asList(Tag.of(CACHE, APP_NAME),
                             Tag.of(PREFIX, KEY_PREFIX),
                             Tag.of(CALL_TYPE_TAG, WRITE),
                             Tag.of(CALL_TAG, SET_OPERATION));
    }

    @AfterEach
    void tearDown() {
        instance.getAllTimers().clear();
        setField(Spectator.globalRegistry(), "registries", new ArrayList<>());
    }

    @Test
    void testSize() {
        assertThat(binder.size()).isNull();
    }

    @Test
    void testHitCount() {
        assertThat(binder.hitCount()).isEqualTo(3);
    }

    @Test
    void testMissCount() {
        assertThat(binder.missCount()).isEqualTo(3);
    }

    @Test
    void testEvictionCount() {
        assertThat(binder.evictionCount()).isEqualTo(3);
    }

    @Test
    void testPutCount() {
        assertThat(binder.putCount()).isEqualTo(3);
    }
}
