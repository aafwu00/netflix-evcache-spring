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

package com.github.aafwu00.evcache.client.spring;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * {@link Cache} implementation on top of an {@link com.netflix.evcache.EVCache} instance.
 *
 * @author Taeho Kim
 */
public class EVCache extends AbstractValueAdaptingCache {
    private final String name;
    private final com.netflix.evcache.EVCache cache;
    private final ConversionService conversionService;

    public EVCache(final String name,
                   final com.netflix.evcache.EVCache cache,
                   final ConversionService conversionService,
                   final boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.cache = requireNonNull(cache);
        this.conversionService = requireNonNull(conversionService);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getCachePrefix() {
        return cache.getCachePrefix();
    }

    public String getAppName() {
        return cache.getAppName();
    }

    @Override
    public com.netflix.evcache.EVCache getNativeCache() {
        return cache;
    }

    @Override
    protected Object lookup(final Object key) {
        try {
            return cache.get(createKey(key));
        } catch (final com.netflix.evcache.EVCacheException ex) {
            throw new EVCacheGetException(key, ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key, final Callable<T> valueLoader) {
        final Object cached = lookup(key);
        if (nonNull(cached)) {
            return (T) fromStoreValue(cached);
        }
        try {
            final T call = valueLoader.call();
            put(key, call);
            return (T) fromStoreValue(call);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(final Object key, final Object value) {
        try {
            cache.set(createKey(key), toStoreValue(value));
        } catch (final com.netflix.evcache.EVCacheException ex) {
            throw new EVCachePutException(key, value, ex);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(final Object key, final Object value) {
        final Object result = get(key, () -> toStoreValue(value));
        return toValueWrapper(result);
    }

    @Override
    public void evict(final Object key) {
        try {
            cache.delete(createKey(key));
        } catch (final com.netflix.evcache.EVCacheException ex) {
            throw new EVCacheEvictException(key, ex);
        }
    }

    private String createKey(final Object key) {
        return convertKey(key);
    }

    private String convertKey(final Object key) {
        Assert.notNull(key, "Key cannot be null");
        final TypeDescriptor source = TypeDescriptor.forObject(key);
        if (conversionService.canConvert(source, TypeDescriptor.valueOf(String.class))) {
            return conversionService.convert(key, String.class);
        }
        return key.toString();
    }

    @Override
    public void clear() {
        throw new EVCacheClearException();
    }
}
