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

package com.github.aafwu00.evcache.client.spring.cloud;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.github.aafwu00.evcache.client.spring.EVCacheManager;
import com.github.aafwu00.evcache.client.spring.EVCachePostConstructCustomizer;

import brave.Span;
import brave.Tracer;
import brave.Tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.cloud.sleuth.util.SpanNameUtil.toLowerHyphen;

class EVCacheManagerTraceCustomizerTest {
    private Tracing tracing;
    private Tracer tracer;
    private EVCacheManagerTraceCustomizer customizer;
    private Cache traceableCache;
    private EVCache evcache;
    private com.netflix.evcache.EVCache nativeCache;
    private Span currentSpan;
    private Span span;

    @BeforeEach
    void setUp() {
        tracing = mock(Tracing.class);
        tracer = mock(Tracer.class);
        doReturn(tracer).when(tracing).tracer();
        customizer = new EVCacheManagerTraceCustomizer(tracing);
        evcache = mock(EVCache.class);
        nativeCache = mock(com.netflix.evcache.EVCache.class);
        doReturn(nativeCache).when(evcache).getNativeCache();
        traceableCache = new EVCacheManagerTraceCustomizer.SleuthTraceableCache(evcache, tracing);
        currentSpan = mock(Span.class);
        span = mock(Span.class);
    }

    @Test
    void should_be_traced_when_get() {
        assertThatTraceable("get", "k", cache -> cache.get("k"));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_get() {
        assertThatTraceable("get", "k", cache -> cache.get("k"), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_get() {
        final SimpleValueWrapper result = new SimpleValueWrapper("v");
        doReturn(result).when(evcache).get("k");
        assertThat(traceableCache.get("k")).isEqualTo(result);
        verify(evcache).get("k");
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_get_with_class() {
        assertThatTraceable("get", "k", cache -> cache.get("k", String.class));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_get_with_class() {
        assertThatTraceable("get", "k", cache -> cache.get("k", String.class), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_get_with_class() {
        doReturn("v").when(evcache).get("k", String.class);
        assertThat(traceableCache.get("k", String.class)).isEqualTo("v");
        verify(evcache).get("k", String.class);
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_get_with_callable() {
        final Callable<String> callable = mock(Callable.class);
        assertThatTraceable("get", "k", cache -> cache.get("k", callable));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_get_with_callable() {
        final Callable<String> callable = mock(Callable.class);
        assertThatTraceable("get", "k", cache -> cache.get("k", callable), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_get_with_callable() {
        final Callable<String> callable = mock(Callable.class);
        doReturn("v").when(evcache).get("k", callable);
        assertThat(traceableCache.get("k", callable)).isEqualTo("v");
        verify(evcache).get("k", callable);
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_put() {
        assertThatTraceable("put", "k", cache -> cache.put("k", "v"));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_put() {
        assertThatTraceable("put", "k", cache -> cache.put("k", "v"), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_put() {
        traceableCache.put("k", "v");
        verify(evcache).put("k", "v");
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_put_if_absent() {
        assertThatTraceable("putIfAbsent", "k", cache -> cache.putIfAbsent("k", "v"));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_put_if_absent() {
        assertThatTraceable("putIfAbsent", "k", cache -> cache.putIfAbsent("k", "v"), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_put_if_absent() {
        final SimpleValueWrapper result = new SimpleValueWrapper("v");
        doReturn(result).when(evcache).putIfAbsent("k", "v");
        assertThat(traceableCache.putIfAbsent("k", "v")).isEqualTo(result);
        verify(evcache).putIfAbsent("k", "v");
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_evict() {
        assertThatTraceable("evict", "k", cache -> cache.evict("k"));
    }

    @Test
    void should_be_traced_when_thrown_exception_to_evict() {
        assertThatTraceable("evict", "k", cache -> cache.evict("k"), new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_evict() {
        traceableCache.evict("k");
        verify(evcache).evict("k");
        verifyNotTraceable();
    }

    @Test
    void should_be_traced_when_clear() {
        assertThatTraceable("clear", "*", Cache::clear);
    }

    @Test
    void should_be_traced_when_thrown_exception_to_clear() {
        assertThatTraceable("clear", "*", Cache::clear, new RuntimeException());
    }

    @Test
    void should_not_be_traced_when_clear() {
        doReturn(currentSpan).when(tracer).currentSpan();
        doReturn(true).when(currentSpan).isNoop();
        traceableCache.clear();
        verify(evcache).clear();
        verifyNotTraceable();
    }

    private void verifyNotTraceable() {
        verify(currentSpan, never()).tag(anyString(), anyString());
        verify(span, never()).tag(anyString(), anyString());
        verify(span, never()).annotate(anyString());
        verify(span, never()).finish();
        verify(span, never()).finish(anyLong());
    }

    private void assertThatTraceable(final String operation,
                                     final String key,
                                     final Consumer<Cache> execution) {
        doReturn(currentSpan).when(tracer).currentSpan();
        doReturn(false).when(currentSpan).isNoop();
        doReturn(span).when(tracer).newTrace();
        doReturn(span).when(span).kind(Span.Kind.CLIENT);
        doReturn(span).when(span).remoteServiceName("AppName");
        doReturn(span).when(span).name(toLowerHyphen(operation));
        doReturn(span).when(span).tag("cache.prefix", "prefix");
        doReturn(span).when(span).tag("cache.key", key);
        doReturn(span).when(span).start();
        doReturn("AppName").when(nativeCache).getAppName();
        doReturn("prefix").when(nativeCache).getCachePrefix();
        execution.accept(traceableCache);
        final InOrder inOrder = inOrder(tracer, currentSpan, span, evcache);
        inOrder.verify(tracer, times(2)).currentSpan();
        inOrder.verify(currentSpan).isNoop();
        inOrder.verify(tracer).newTrace();
        inOrder.verify(span).kind(Span.Kind.CLIENT);
        inOrder.verify(span).remoteServiceName("AppName");
        inOrder.verify(span).name(toLowerHyphen(operation));
        inOrder.verify(span).tag("cache.prefix", "prefix");
        inOrder.verify(span).tag("cache.key", key);
        inOrder.verify(span).start();
        execution.accept(inOrder.verify(evcache));
        inOrder.verify(span, never()).error(any());
        inOrder.verify(span).finish();
    }

    private void assertThatTraceable(final String operation,
                                     final String key,
                                     final Consumer<Cache> execution,
                                     final Throwable throwable) {
        doReturn(currentSpan).when(tracer).currentSpan();
        doReturn(false).when(currentSpan).isNoop();
        doReturn(span).when(tracer).newTrace();
        doReturn(span).when(span).kind(Span.Kind.CLIENT);
        doReturn(span).when(span).remoteServiceName("AppName");
        doReturn(span).when(span).name(toLowerHyphen(operation));
        doReturn(span).when(span).tag("cache.prefix", "prefix");
        doReturn(span).when(span).tag("cache.key", key);
        doReturn(span).when(span).start();
        doReturn("AppName").when(nativeCache).getAppName();
        doReturn("prefix").when(nativeCache).getCachePrefix();
        execution.accept(doThrow(throwable).when(evcache));
        assertThatThrownBy(() -> execution.accept(traceableCache)).isEqualTo(throwable);
        final InOrder inOrder = inOrder(tracer, currentSpan, span, evcache);
        inOrder.verify(tracer, times(2)).currentSpan();
        inOrder.verify(currentSpan).isNoop();
        inOrder.verify(tracer).newTrace();
        inOrder.verify(span).kind(Span.Kind.CLIENT);
        inOrder.verify(span).remoteServiceName("AppName");
        inOrder.verify(span).name(toLowerHyphen(operation));
        inOrder.verify(span).tag("cache.prefix", "prefix");
        inOrder.verify(span).tag("cache.key", key);
        inOrder.verify(span).start();
        execution.accept(inOrder.verify(evcache));
        inOrder.verify(span).error(throwable);
        inOrder.verify(span).finish();
    }

    @Test
    void customize() {
        final EVCacheManager cacheManager = mock(EVCacheManager.class);
        customizer.customize(cacheManager);
        verify(cacheManager).addCustomizer(any(EVCachePostConstructCustomizer.class));
    }
}
