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

import java.util.Optional;
import java.util.function.Consumer;

import com.netflix.evcache.event.EVCacheEvent;
import com.netflix.evcache.event.EVCacheEventListener;

import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.Tracing;

import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.springframework.cloud.sleuth.util.SpanNameUtil.shorten;

/**
 * @author Taeho Kim
 */
public class TraceableListener implements EVCacheEventListener {
    private static final String SPAN = "trace-span";
    private static final String SCOPE = "trace-scope";
    private static final String STATUS = "status";
    private final Tracing tracing;

    public TraceableListener(final Tracing tracing) {
        this.tracing = requireNonNull(tracing);
    }

    @Override
    public void onStart(final EVCacheEvent event) {
        if (isNotTraceable()) {
            return;
        }
        final Span span = createSpan(event).start(event.getStartTime());
        event.setAttribute(SPAN, span);
        final SpanInScope scope = tracer().withSpanInScope(span);
        event.setAttribute(SCOPE, scope);
    }

    private boolean isNotTraceable() {
        final Span span = currentSpan();
        return isNull(span) || span.isNoop();
    }

    private Span currentSpan() {
        return tracer().currentSpan();
    }

    private Tracer tracer() {
        return tracing.tracer();
    }

    private Span createSpan(final EVCacheEvent event) {
        return tracer().nextSpan()
                       .name(shorten(lowerCase(event.getCall().name())))
                       .kind(Span.Kind.CLIENT)
                       .remoteServiceName(event.getAppName())
                       .tag("call", event.getCall().name())
                       .tag("app-name", event.getAppName())
                       .tag("key-prefix", event.getCacheName())
                       .tag("ttl", valueOf(event.getTTL()))
                       .tag("keys", valueOf(event.getEVCacheKeys()));
    }

    @Override
    public void onComplete(final EVCacheEvent event) {
        close(event, span -> span.tag(STATUS, valueOf(event.getAttribute(STATUS))));
    }

    private void close(final EVCacheEvent event, final Consumer<Span> consumer) {
        getSpan(event).ifPresent(consumer.andThen(Span::finish));
        getScope(event).ifPresent(SpanInScope::close);
    }

    private Optional<Span> getSpan(final EVCacheEvent event) {
        return Optional.ofNullable(event.getAttribute(SPAN))
                       .map(Span.class::cast);
    }

    private Optional<SpanInScope> getScope(final EVCacheEvent event) {
        return Optional.ofNullable(event.getAttribute(SCOPE))
                       .map(SpanInScope.class::cast);
    }

    @Override
    public void onError(final EVCacheEvent event, final Throwable error) {
        close(event, span -> span.error(error));
    }

    @Override
    public boolean onThrottle(final EVCacheEvent event) {
        return false;
    }
}
