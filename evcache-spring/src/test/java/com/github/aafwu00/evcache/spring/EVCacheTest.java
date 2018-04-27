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

package com.github.aafwu00.evcache.spring;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheTest {
    private com.netflix.evcache.EVCache source;
    private ConversionService converterService;
    private EVCache cache;

    @BeforeEach
    void setUp() {
        source = mock(com.netflix.evcache.EVCache.class);
        converterService = mock(ConversionService.class);
        cache = new EVCache(source, converterService, true);
        doReturn(false).when(converterService)
                       .canConvert(TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
    }

    @Test
    void name() {
        doReturn("name").when(source).getCachePrefix();
        assertThat(cache.getName()).isEqualTo("name");
    }

    @Test
    void nativeCache() {
        assertThat(cache.getNativeCache()).isEqualTo(source);
    }

    @Test
    void createKey() {
        assertAll(
            () -> {
                doReturn(true).when(converterService)
                              .canConvert(TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
                doReturn("1").when(converterService).convert(1, String.class);
                doReturn(1).when(source).get("1");
                assertThat(cache.lookup(1)).isEqualTo(1);
            },
            () -> {
                doReturn(false).when(converterService)
                               .canConvert(TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
                doReturn(1).when(source).get("1");
                assertThat(cache.lookup(1)).isEqualTo(1);
            },
            () -> {
                doReturn(false).when(converterService)
                               .canConvert(TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
                doReturn(1).when(source).get(DigestUtils.sha256Hex("1"));
                assertThat(new EVCache(source, converterService, true).lookup(1)).isEqualTo(1);
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
            }
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
            }
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
            }
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
            }
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
            }
        );
    }
}
