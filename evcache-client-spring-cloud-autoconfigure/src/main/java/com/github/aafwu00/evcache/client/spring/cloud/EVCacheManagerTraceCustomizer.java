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
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;

import com.github.aafwu00.evcache.client.spring.EVCache;
import com.github.aafwu00.evcache.client.spring.EVCacheManager;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import zipkin2.Endpoint;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.springframework.cloud.sleuth.util.SpanNameUtil.toLowerHyphen;

/**
 * @author Taeho Kim
 */
public class EVCacheManagerTraceCustomizer implements CacheManagerCustomizer<EVCacheManager> {
    private final Tracing tracing;

    public EVCacheManagerTraceCustomizer(final Tracing tracing) {
        this.tracing = requireNonNull(tracing);
    }

    @Override
    public void customize(final EVCacheManager cacheManager) {
        cacheManager.addCustomizer(cache -> new SleuthTraceableCache(cache, tracing));
    }

    static class SleuthTraceableCache implements EVCache {
        private static final String GET = "get";
        private static final String PUT = "put";
        private static final String PUT_IF_ABSENT = "putIfAbsent";
        private static final String EVICT = "evict";
        private static final String CLEAR = "clear";
        private static final String KEY_ALL = "*";
        private final EVCache cache;
        private final Tracing tracing;

        SleuthTraceableCache(final EVCache cache, final Tracing tracing) {
            this.cache = requireNonNull(cache);
            this.tracing = requireNonNull(tracing);
        }

        private <T> T execute(final String operation, final Object key, final Supplier<T> callback) {
            if (isNotTraceable()) {
                return callback.get();
            }
            final Span span = createSpan(operation, key);
            try (Tracer.SpanInScope scope = tracing.tracer().withSpanInScope(span.start())) {
                return callback.get();
                // CHECKSTYLE:OFF
            } catch (Exception ex) {
                // CHECKSTYLE:ON
                span.error(ex);
                throw ex;
            } finally {
                span.finish();
            }
        }

        private boolean isNotTraceable() {
            return isNull(currentSpan()) || currentSpan().isNoop();
        }

        private Span currentSpan() {
            return tracing.tracer().currentSpan();
        }

        private Span createSpan(final String name, final Object key) {
            return tracing.tracer()
                          .newTrace()
                          .kind(Span.Kind.CLIENT)
                          .remoteEndpoint(Endpoint.newBuilder()
                                                  .serviceName(getNativeCache().getAppName())
                                                  .build())
                          .name(toLowerHyphen(name))
                          .tag("cache.prefix", getNativeCache().getCachePrefix())
                          .tag("cache.key", String.valueOf(key));
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
