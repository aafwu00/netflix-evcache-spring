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

package com.github.aafwu00.netflix.evcache.spring.cloud;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import com.github.aafwu00.netflix.evcache.spring.EVCache;
import com.github.aafwu00.netflix.evcache.spring.EVCacheManager;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.springframework.cloud.sleuth.Span.CLIENT_RECV;
import static org.springframework.cloud.sleuth.Span.CLIENT_SEND;
import static org.springframework.cloud.sleuth.Span.SPAN_PEER_SERVICE_TAG_NAME;

/**
 * @author Taeho Kim
 */
public class EVCacheManagerTraceCustomizer implements CacheManagerCustomizer<EVCacheManager> {
    private final Tracer tracer;
    private final ErrorParser errorParser;

    public EVCacheManagerTraceCustomizer(final Tracer tracer, final ErrorParser errorParser) {
        this.tracer = requireNonNull(tracer);
        this.errorParser = requireNonNull(errorParser);
    }

    @Override
    public void customize(final EVCacheManager cacheManager) {
        cacheManager.setCustomizer(cache -> new SleuthTraceableCache(cache, tracer, errorParser));
    }

    static class SleuthTraceableCache implements Cache {
        private static final String GET = "get";
        private static final String PUT = "put";
        private static final String PUT_IF_ABSENT = "putIfAbsent";
        private static final String EVICT = "evict";
        private static final String CLEAR = "clear";
        private static final String KEY_ALL = "*";
        private final EVCache cache;
        private final Tracer tracer;
        private final ErrorParser errorParser;

        SleuthTraceableCache(final EVCache cache, final Tracer tracer, final ErrorParser errorParser) {
            this.cache = requireNonNull(cache);
            this.tracer = requireNonNull(tracer);
            this.errorParser = requireNonNull(errorParser);
        }

        private <T> T execute(final String operation, final Object key, final Supplier<T> callback) {
            if (isNotTraceable()) {
                return callback.get();
            }
            final Span span = createSpan(operation);
            try {
                send(span, key);
                return callback.get();
                // CHECKSTYLE:OFF
            } catch (Exception ex) {
                // CHECKSTYLE:ON
                parseErrorTags(span, ex);
                throw ex;
            } finally {
                receive(span);
                close(span);
            }
        }

        private boolean isNotTraceable() {
            return isNull(currentSpan()) || !currentSpan().isExportable();
        }

        private Span currentSpan() {
            return tracer.getCurrentSpan();
        }

        private Span createSpan(final String name) {
            return tracer.createSpan(name);
        }

        private void send(final Span span, final Object key) {
            span.tag("cache.prefix", getNativeCache().getCachePrefix());
            span.tag("cache.key", String.valueOf(key));
            span.logEvent(CLIENT_SEND);
        }

        private void parseErrorTags(final Span span, final Throwable throwable) {
            errorParser.parseErrorTags(span, throwable);
        }

        private void receive(final Span span) {
            span.logEvent(CLIENT_RECV);
            span.tag(SPAN_PEER_SERVICE_TAG_NAME, getNativeCache().getAppName());
        }

        private void close(final Span span) {
            tracer.close(span);
        }

        @Override
        public String getName() {
            return cache.getName();
        }

        @Override
        public com.netflix.evcache.EVCache getNativeCache() {
            return cache.getNativeCache();
        }

        @Override
        public ValueWrapper get(final Object key) {
            return execute(GET, key, () -> cache.get(key));
        }

        @Override
        public <T> T get(final Object key, final Class<T> type) {
            return execute(GET, key, () -> cache.get(key, type));
        }

        @Override
        public <T> T get(final Object key, final Callable<T> valueLoader) {
            return execute(GET, key, () -> cache.get(key, valueLoader));
        }

        @Override
        public void put(final Object key, final Object value) {
            execute(PUT, key, () -> {
                cache.put(key, value);
                return empty();
            });
        }

        @Override
        public ValueWrapper putIfAbsent(final Object key, final Object value) {
            return execute(PUT_IF_ABSENT, key, () -> cache.putIfAbsent(key, value));
        }

        @Override
        public void evict(final Object key) {
            execute(EVICT, key, () -> {
                cache.evict(key);
                return empty();
            });
        }

        @Override
        public void clear() {
            execute(CLEAR, KEY_ALL, () -> {
                cache.clear();
                return empty();
            });
        }
    }
}
