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

package com.github.aafwu00.evcache.client.spring.cloud;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.evcache.EVCache;
import com.netflix.evcache.EVCacheKey;
import com.netflix.evcache.event.EVCacheEvent;

import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class TraceableListenerTest {
    private Tracing tracing;
    private Tracer tracer;
    private Span span;
    private SpanInScope spanInScope;
    private Scope scope;
    private EVCacheEvent event;
    private TraceableListener listener;

    @BeforeEach
    void setUp() {
        tracing = mock(Tracing.class);
        tracer = mock(Tracer.class);
        span = mock(Span.class);
        event = mock(EVCacheEvent.class);
        scope = mock(Scope.class);
        doReturn(tracer).when(tracing).tracer();
        doReturn(span).when(tracer).currentSpan();
        final CurrentTraceContext currentTraceContext = mock(CurrentTraceContext.class);
        doReturn(scope).when(currentTraceContext).newScope(null);
        spanInScope = Tracing.newBuilder().currentTraceContext(currentTraceContext).build().tracer().withSpanInScope(span);
        doReturn(spanInScope).when(tracer).withSpanInScope(span);
        listener = new TraceableListener(tracing);
    }

    @Test
    void testOnStartWhenNotTraceable() {
        doReturn(null).when(tracer).currentSpan();
        listener.onStart(event);
        verify(event, never()).setAttribute(any(), any());
        doReturn(true).when(span).isNoop();
        verify(event, never()).setAttribute(any(), any());
    }

    @Test
    void testOnStartWhenTraceable() {
        final long startTime = System.currentTimeMillis();
        doReturn(false).when(span).isNoop();
        doReturn(span).when(tracer).nextSpan();
        doReturn(EVCache.Call.GET).when(event).getCall();
        doReturn(span).when(span).name("get");
        doReturn(span).when(span).kind(Span.Kind.CLIENT);
        doReturn("appName").when(event).getAppName();
        doReturn(span).when(span).remoteServiceName("appName");
        doReturn(span).when(span).tag("call", "GET");
        doReturn(span).when(span).tag("app-name", "appName");
        doReturn("cacheName").when(event).getCacheName();
        doReturn(span).when(span).tag("key-prefix", "cacheName");
        doReturn(100).when(event).getTTL();
        doReturn(span).when(span).tag("ttl", "100");
        final EVCacheKey key = new EVCacheKey("key", "cKey", "hKey");
        final List<EVCacheKey> keys = singletonList(key);
        doReturn(keys).when(event).getEVCacheKeys();
        doReturn(span).when(span).tag("keys", String.valueOf(keys));
        doReturn(startTime).when(event).getStartTime();
        doReturn(span).when(span).start(startTime);
        listener.onStart(event);
        verify(span).start(startTime);
        verify(event).setAttribute("trace-span", span);
        verify(event).setAttribute("trace-scope", spanInScope);
    }

    @Test
    void testOnComplete() {
        doReturn(span).when(event).getAttribute("trace-span");
        doReturn(spanInScope).when(event).getAttribute("trace-scope");
        doReturn("success").when(event).getAttribute("state");
        doReturn(span).when(span).tag("state", "success");
        listener.onComplete(event);
        verify(span).finish();
        verify(scope).close();
    }

    @Test
    void testOnError() {
        final Throwable error = mock(Throwable.class);
        doReturn(span).when(event).getAttribute("trace-span");
        doReturn(spanInScope).when(event).getAttribute("trace-scope");
        doReturn("success").when(event).getAttribute("state");
        doReturn(span).when(span).error(error);
        listener.onError(event, error);
        verify(span).finish();
        verify(scope).close();
    }

    @Test
    void testOnThrottle() {
        assertThat(listener.onThrottle(null)).isFalse();
    }
}
