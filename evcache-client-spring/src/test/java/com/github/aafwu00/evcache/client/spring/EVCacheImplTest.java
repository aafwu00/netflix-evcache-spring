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

import com.netflix.evcache.EVCacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheImplTest {
    private com.netflix.evcache.EVCache source;
    private EVCacheImpl cache;
    private Callable<Integer> callable;

    @BeforeEach
    void setUp() {
        source = mock(com.netflix.evcache.EVCache.class);
        cache = new EVCacheImpl("name", source, true);
        callable = mock(Callable.class);
    }

    @Test
    void should_be_same_origin_NativeCache() {
        assertThat(cache.getNativeCache()).isEqualTo(source);
    }

    @Test
    void should_be_same_origin_name() {
        assertThat(cache.getName()).isEqualTo("name");
    }

    @Test
    void should_be_same_origin_appName() {
        doReturn("appName").when(source).getAppName();
        assertThat(cache.getAppName()).isEqualTo("appName");
    }

    @Test
    void should_be_same_origin_keyPrefix() {
        doReturn("prefix").when(source).getCachePrefix();
        assertThat(cache.getKeyPrefix()).isEqualTo("prefix");
    }

    @Test
    void should_be_get_when_lookup() throws EVCacheException {
        doReturn(1).when(source).get("1");
        assertThat(cache.lookup(1)).isEqualTo(1);
    }

    @Test
    void should_be_throw_EVCacheGetException_when_lookup_throw_EVCacheException() throws EVCacheException {
        doThrow(EVCacheException.class).when(source).get("1");
        assertThatThrownBy(() -> cache.lookup("1")).isExactlyInstanceOf(EVCacheGetException.class);
    }

    @Test
    void should_be_thrown_IllegalArgumentException_when_lookup_key_is_null() {
        assertThatThrownBy(() -> cache.lookup(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                    .hasMessage("Key cannot be null");
    }


    @Test
    void should_be_set_when_put() throws EVCacheException {
        cache.put(1, 2);
        verify(source).set("1", 2);
    }

    @Test
    void should_be_throw_EVCachePutException_when_get_throw_EVCacheException() throws EVCacheException {
        doThrow(EVCacheException.class).when(source).set("1", "1");
        assertThatThrownBy(() -> cache.put("1", "1")).isExactlyInstanceOf(EVCachePutException.class);
    }

    @Test
    void should_be_thrown_IllegalArgumentException_when_put_key_is_null() {
        assertThatThrownBy(() -> cache.put(null, null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                       .hasMessage("Key cannot be null");
    }

    @Test
    void should_be_not_set_when_putIfAbsent_key_exists() throws EVCacheException {
        doReturn(1).when(source).get("1");
        assertThat(cache.putIfAbsent(1, null).get()).isEqualTo(1);
        verify(source, never()).set(any(), any());
    }

    @Test
    void should_be_set_when_putIfAbsent_key_is_not_exists() throws EVCacheException {
        doReturn(null).when(source).get("1");
        assertThat(cache.putIfAbsent(1, 2).get()).isEqualTo(2);
        verify(source).set("1", 2);
    }

    @Test
    void should_be_thrown_IllegalArgumentException_when_putIfAbsent_key_is_null() {
        assertThatThrownBy(() -> cache.putIfAbsent(null, null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                               .hasMessage("Key cannot be null");
    }

    @Test
    void should_be_delete_when_evict() throws EVCacheException {
        cache.evict(1);
        verify(source).delete("1");
    }

    @Test
    void should_be_throw_IllegalArgumentException_when_evict_key_is_null() {
        assertThatThrownBy(() -> cache.evict(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                   .hasMessage("Key cannot be null");
    }

    @Test
    void should_be_throw_EVCacheEvictException_when_evict_throw_EVCacheException() throws EVCacheException {
        doThrow(EVCacheException.class).when(source).delete("1");
        assertThatThrownBy(() -> cache.evict("1")).isExactlyInstanceOf(EVCacheEvictException.class);
    }

    @Test
    void clear() {
        assertThatThrownBy(() -> cache.clear()).isExactlyInstanceOf(EVCacheClearException.class);
    }

    @Test
    void should_be_not_set_when_callable_key_is_exists() throws EVCacheException {
        doReturn(1).when(source).get("1");
        assertThat(cache.get(1, callable)).isEqualTo(1);
        verify(source, never()).set(any(), any());
    }

    @Test
    void should_be_set_when_callable_key_is_not_exists() throws Exception {
        doReturn(null).when(source).get("1");
        doReturn(2).when(callable).call();
        assertThat(cache.get(1, callable)).isEqualTo(2);
        verify(source).set("1", 2);
    }

    @Test
    void should_be_thrown_ValueRetrievalException_when_callable_throw_Exception() throws Exception {
        doReturn(null).when(source).get("1");
        doThrow(RuntimeException.class).when(callable).call();
        assertThatThrownBy(() -> cache.get(1, callable)).isExactlyInstanceOf(Cache.ValueRetrievalException.class);
        verify(source, never()).set(any(), any());
    }

    @Test
    void should_be_thrown_IllegalArgumentException_when_callable_key_is_null() {
        assertThatThrownBy(() -> cache.get(null, callable)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                           .hasMessage("Key cannot be null");
    }
}
