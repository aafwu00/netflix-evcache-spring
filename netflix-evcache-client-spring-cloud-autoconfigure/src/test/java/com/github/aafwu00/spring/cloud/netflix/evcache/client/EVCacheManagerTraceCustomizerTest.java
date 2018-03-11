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

package com.github.aafwu00.spring.cloud.netflix.evcache.client;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import com.github.aafwu00.spring.netflix.evcache.client.EVCache;
import com.github.aafwu00.spring.netflix.evcache.client.EVCacheManager;
import com.github.aafwu00.spring.netflix.evcache.client.EVCachePostConstructCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.cloud.sleuth.Span.CLIENT_RECV;
import static org.springframework.cloud.sleuth.Span.CLIENT_SEND;
import static org.springframework.cloud.sleuth.Span.SPAN_PEER_SERVICE_TAG_NAME;

class EVCacheManagerTraceCustomizerTest {
    private Tracer tracer;
    private ErrorParser errorParser;
    private EVCacheManagerTraceCustomizer customizer;
    private Cache traceableCache;
    private EVCache evcache;
    private com.netflix.evcache.EVCache nativeCache;
    private Span currentSpan;
    private Span span;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        errorParser = mock(ErrorParser.class);
        customizer = new EVCacheManagerTraceCustomizer(tracer, errorParser);
        evcache = mock(EVCache.class);
        nativeCache = mock(com.netflix.evcache.EVCache.class);
        doReturn(nativeCache).when(evcache).getNativeCache();
        traceableCache = new EVCacheManagerTraceCustomizer.SleuthTraceableCache(evcache, tracer, errorParser);
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
        doReturn(currentSpan).when(tracer).getCurrentSpan();
        doReturn(false).when(currentSpan).isExportable();
        traceableCache.clear();
        verify(evcache).clear();
        verifyNotTraceable();
    }

    private void verifyNotTraceable() {
        verify(currentSpan, never()).tag(anyString(), anyString());
        verify(currentSpan, never()).logEvent(anyString());
        verify(span, never()).tag(anyString(), anyString());
        verify(span, never()).logEvent(anyString());
        verify(tracer, never()).close(any(Span.class));
    }

    private void assertThatTraceable(final String operation,
                                     final String key,
                                     final Consumer<Cache> execution) {
        doReturn(currentSpan).when(tracer).getCurrentSpan();
        doReturn(true).when(currentSpan).isExportable();
        doReturn(span).when(tracer).createSpan(operation);
        doReturn("AppName").when(nativeCache).getAppName();
        doReturn("prefix").when(nativeCache).getCachePrefix();
        execution.accept(traceableCache);
        final InOrder inOrder = inOrder(span, evcache, tracer);
        inOrder.verify(span).tag("cache.prefix", "prefix");
        inOrder.verify(span).tag("cache.key", key);
        inOrder.verify(span).logEvent(CLIENT_SEND);
        execution.accept(inOrder.verify(evcache));
        inOrder.verify(span).logEvent(CLIENT_RECV);
        inOrder.verify(span).tag(SPAN_PEER_SERVICE_TAG_NAME, "AppName");
        inOrder.verify(tracer).close(span);
    }

    private void assertThatTraceable(final String operation,
                                     final String key,
                                     final Consumer<Cache> execution,
                                     final Throwable throwable) {
        doReturn(currentSpan).when(tracer).getCurrentSpan();
        doReturn(true).when(currentSpan).isExportable();
        doReturn(span).when(tracer).createSpan(operation);
        doReturn("AppName").when(nativeCache).getAppName();
        doReturn("prefix").when(nativeCache).getCachePrefix();
        execution.accept(doThrow(throwable).when(evcache));
        assertThatThrownBy(() -> execution.accept(traceableCache)).isEqualTo(throwable);
        final InOrder inOrder = inOrder(span, evcache, errorParser, tracer);
        inOrder.verify(span).tag("cache.prefix", "prefix");
        inOrder.verify(span).tag("cache.key", key);
        inOrder.verify(span).logEvent(CLIENT_SEND);
        execution.accept(inOrder.verify(evcache));
        inOrder.verify(errorParser).parseErrorTags(span, throwable);
        inOrder.verify(span).logEvent(CLIENT_RECV);
        inOrder.verify(span).tag(SPAN_PEER_SERVICE_TAG_NAME, "AppName");
        inOrder.verify(tracer).close(span);
    }

    @Test
    void customize() {
        final EVCacheManager cacheManager = mock(EVCacheManager.class);
        customizer.customize(cacheManager);
        verify(cacheManager).setCustomizer(any(EVCachePostConstructCustomizer.class));
    }
}
