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

package com.github.aafwu00.evcache.client.spring;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * {@link Cache} implementation on top of an {@link com.netflix.evcache.EVCache} instance.
 *
 * @author Taeho Kim
 */
public class EVCacheImpl extends AbstractValueAdaptingCache implements EVCache {
    private final String name;
    private final com.netflix.evcache.EVCache cache;

    public EVCacheImpl(final String name, final com.netflix.evcache.EVCache cache, final boolean allowNullValues) {
        super(allowNullValues);
        this.name = requireNonNull(name);
        this.cache = requireNonNull(cache);
    }

    @Override
    public String getName() {
        return name;
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

    /**
     * Not Support Synchronize
     *
     * @see org.springframework.cache.annotation.Cacheable#sync()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key, final Callable<T> valueLoader) {
        final Object cached = lookup(key);
        if (nonNull(cached)) {
            return (T) fromStoreValue(cached);
        }
        try {
            final T value = valueLoader.call();
            put(key, value);
            return (T) fromStoreValue(value);
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
        return key.toString();
    }

    @Override
    public void clear() {
        throw new EVCacheClearException();
    }
}
