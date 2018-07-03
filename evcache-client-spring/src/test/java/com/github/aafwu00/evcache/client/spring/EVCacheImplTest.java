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
import java.util.concurrent.Future;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
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

    @BeforeEach
    void setUp() {
        source = mock(com.netflix.evcache.EVCache.class);
        cache = new EVCacheImpl("name", source, true);
    }

    @Test
    void nativeCache() {
        assertThat(cache.getNativeCache()).isEqualTo(source);
    }

    @Test
    void name() {
        assertThat(cache.getName()).isEqualTo("name");
    }

    @Test
    void appName() {
        doReturn("appName").when(source).getAppName();
        assertThat(cache.getAppName()).isEqualTo("appName");
    }

    @Test
    void cachePrefix() {
        doReturn("prefix").when(source).getCachePrefix();
        assertThat(cache.getCachePrefix()).isEqualTo("prefix");
    }

    @Test
    void createKey() {
        assertAll(
            () -> {
                doReturn(1).when(source).get("1");
                assertThat(cache.lookup(1)).isEqualTo(1);
            },
            () -> {
                doReturn(1).when(source).get("1");
                assertThat(cache.lookup(1)).isEqualTo(1);
            },
            () -> {
                doReturn(1).when(source).get(DigestUtils.sha256Hex("1"));
                assertThat(new EVCacheImpl("name", source, true).lookup(1)).isEqualTo(1);
            }
        );
    }

    @Test
    void lookup() {
        assertAll(
            () -> {
                doReturn(1).when(source).get("1");
                assertThat(cache.lookup(1)).isEqualTo(1);
            },
            () -> {
                doThrow(com.netflix.evcache.EVCacheException.class).when(source).get("1");
                assertThatThrownBy(() -> cache.lookup(1)).isExactlyInstanceOf(EVCacheGetException.class);
            },
            () -> assertThatThrownBy(() -> cache.lookup(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                              .hasMessage("Key cannot be null")
        );
    }

    @Test
    void put() {
        assertAll(
            () -> {
                cache.put(1, 2);
                verify(source).set("1", 2);
            },
            () -> {
                doThrow(com.netflix.evcache.EVCacheException.class).when(source).set("1", 2);
                assertThatThrownBy(() -> cache.put(1, 2)).isExactlyInstanceOf(EVCachePutException.class);
            },
            () -> assertThatThrownBy(() -> cache.put(null, null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                                 .hasMessage("Key cannot be null")
        );
    }

    @Test
    void putIfAbsent() {
        assertAll(
            () -> {
                doReturn(1).when(source).get("1");
                assertThat(cache.putIfAbsent(1, null).get()).isEqualTo(1);
                verify(source, never()).set(any(), any());
            },
            () -> {
                doReturn(null).when(source).get("1");
                assertThat(cache.putIfAbsent(1, 2).get()).isEqualTo(2);
                verify(source).set("1", 2);
            },
            () -> assertThatThrownBy(() -> cache.putIfAbsent(null, null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                                         .hasMessage("Key cannot be null")
        );
    }

    @Test
    void evict() {
        assertAll(
            () -> {
                doReturn(new Future[]{}).when(source).delete("1");
                cache.evict(1);
            },
            () -> {
                doThrow(com.netflix.evcache.EVCacheException.class).when(source).delete("1");
                assertThatThrownBy(() -> cache.evict(1)).isExactlyInstanceOf(EVCacheEvictException.class);
            },
            () -> assertThatThrownBy(() -> cache.evict(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                             .hasMessage("Key cannot be null")
        );
    }

    @Test
    void clear() {
        assertThatThrownBy(() -> cache.clear()).isExactlyInstanceOf(EVCacheClearException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void callableGet() {
        final Callable<Integer> callable = mock(Callable.class);
        assertAll(
            () -> {
                doReturn(1).when(source).get("1");
                assertThat(cache.get(1, callable)).isEqualTo(1);
                verify(source, never()).set(any(), any());
            },
            () -> {
                doReturn(null).when(source).get("1");
                doThrow(RuntimeException.class).when(callable).call();
                assertThatThrownBy(() -> cache.get(1, callable)).isExactlyInstanceOf(Cache.ValueRetrievalException.class);
                verify(source, never()).set(any(), any());
            },
            () -> {
                doReturn(null).when(source).get("1");
                doReturn(2).when(callable).call();
                assertThat(cache.get(1, callable)).isEqualTo(2);
                verify(source).set("1", 2);
            },
            () -> assertThatThrownBy(() -> cache.get(null, callable)).isExactlyInstanceOf(IllegalArgumentException.class)
                                                                     .hasMessage("Key cannot be null")
        );
    }
}
