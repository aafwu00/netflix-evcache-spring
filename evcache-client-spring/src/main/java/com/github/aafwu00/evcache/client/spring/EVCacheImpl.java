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

import com.google.common.util.concurrent.Striped;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

import static java.util.Objects.nonNull;

/**
 * {@link Cache} implementation on top of an {@link com.netflix.evcache.EVCache} instance.
 *
 * @author Taeho Kim
 */
public class EVCacheImpl extends AbstractValueAdaptingCache implements EVCache {
    private final String name;
    private final com.netflix.evcache.EVCache cache;
    private final Striped<Lock> locks;


    /**
     * Create a {@link EVCache} instance with the specified name and the
     * given internal {@link com.netflix.evcache.EVCache} to use.
     *
     * @param name            the name of the cache
     * @param cache           the backing EVCache instance
     * @param allowNullValues whether to accept and convert {@code null}
     * @param striped         the minimum number of stripes (locks) required,
     *                        effected only {@link EVCacheImpl#get(Object, Callable)},
     *                        {@link EVCacheImpl#putIfAbsent(Object, Object)}
     */
    public EVCacheImpl(final String name,
                       final com.netflix.evcache.EVCache cache,
                       final boolean allowNullValues,
                       final int striped) {
        super(allowNullValues);
        Assert.notNull(name, "`name` must not be null");
        Assert.notNull(cache, "`cache` must not be null");
        Assert.state(striped > 0, "`striped` must be positive value");
        this.name = name;
        this.cache = cache;
        this.locks = Striped.lock(striped);
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
        return doGet(asKey(key));
    }

    private String asKey(final Object key) {
        Assert.notNull(key, "Key cannot be null");
        return key.toString();
    }

    private Object doGet(final String key) {
        try {
            return cache.get(key);
        } catch (final com.netflix.evcache.EVCacheException ex) {
            throw new EVCacheGetException(key, ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key, final Callable<T> valueLoader) {
        final String candidateKey = asKey(key);
        final Lock lock = locks.get(candidateKey);
        lock.lock();
        try {
            final Object cached = doGet(candidateKey);
            if (nonNull(cached)) {
                return (T) fromStoreValue(cached);
            }
            final T value = valueLoader.call();
            doSet(candidateKey, value);
            return (T) fromStoreValue(value);
        } catch (final Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(final Object key, final Object value) {
        doSet(asKey(key), value);
    }

    private void doSet(final String key, final Object value) {
        try {
            cache.set(key, toStoreValue(value));
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
        doDelete(asKey(key));
    }

    private void doDelete(final String key) {
        try {
            cache.delete(key);
        } catch (final com.netflix.evcache.EVCacheException ex) {
            throw new EVCacheEvictException(key, ex);
        }
    }

    @Override
    public void clear() {
        throw new EVCacheClearException();
    }
}
