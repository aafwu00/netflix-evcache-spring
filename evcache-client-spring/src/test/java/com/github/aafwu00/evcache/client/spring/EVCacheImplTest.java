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
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.cache.Cache;
import org.springframework.util.StopWatch;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
@SuppressWarnings("unchecked")
class EVCacheImplTest {
    private com.netflix.evcache.EVCache source;
    private EVCacheImpl cache;
    private Callable<Integer> callable;

    @BeforeEach
    void setUp() {
        source = mock(com.netflix.evcache.EVCache.class);
        cache = new EVCacheImpl("name", source, true, 10, false);
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
    void should_be_thrown_EVCacheInvalidKeyException_when_lookup_key_is_null() {
        assertThatThrownBy(() -> cache.lookup(null)).isExactlyInstanceOf(EVCacheInvalidKeyException.class);
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
    void should_be_thrown_EVCacheInvalidKeyException_when_put_key_is_null() {
        assertThatThrownBy(() -> cache.put(null, null)).isExactlyInstanceOf(EVCacheInvalidKeyException.class);
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
    void should_be_thrown_EVCacheInvalidKeyException_when_putIfAbsent_key_is_null() {
        assertThatThrownBy(() -> cache.putIfAbsent(null, null)).isExactlyInstanceOf(EVCacheInvalidKeyException.class);
    }

    @Test
    void should_be_delete_when_evict() throws EVCacheException {
        cache.evict(1);
        verify(source).delete("1");
    }

    @Test
    void should_be_throw_EVCacheInvalidKeyException_when_evict_key_is_null() {
        assertThatThrownBy(() -> cache.evict(null)).isExactlyInstanceOf(EVCacheInvalidKeyException.class);
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
    void should_be_locked_when_callable_key_is_same() throws Exception {
        doAnswer(new AnswersWithDelay(100, new Returns(1)))
            .doReturn(1).when(source).get("1");
        final StopWatch stopWatch = new StopWatch();
        Executors.newFixedThreadPool(1).submit(() -> cache.get(1, callable));
        stopWatch.start();
        assertThat(cache.get(1, callable)).isEqualTo(1);
        stopWatch.stop();
        assertThat(stopWatch.getTotalTimeMillis()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void should_be_not_locked_when_callable_key_is_not_same() throws Exception {
        doAnswer(new AnswersWithDelay(100, new Returns(1))).when(source).get("1");
        doReturn(2).when(source).get("2");
        doReturn(1).when(callable).call();
        Executors.newFixedThreadPool(1).submit(() -> cache.get(1, callable));
        assertTimeoutPreemptively(ofMillis(20), () -> cache.get(2, callable));
    }

    @Test
    void should_be_thrown_ValueRetrievalException_when_callable_throw_Exception() throws Exception {
        doReturn(null).when(source).get("1");
        doThrow(RuntimeException.class).when(callable).call();
        assertThatThrownBy(() -> cache.get(1, callable)).isExactlyInstanceOf(Cache.ValueRetrievalException.class);
        verify(source, never()).set(any(), any());
    }

    @Test
    void should_be_thrown_EVCacheInvalidKeyException_when_key_is_null() {
        assertThatThrownBy(() -> cache.lookup(null)).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                    .hasMessage("Key must not be null");
    }

    @Test
    void should_be_thrown_EVCacheInvalidKeyException_when_key_is_empty() {
        assertThatThrownBy(() -> cache.lookup("")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                  .hasMessage("Key must not be empty");
    }

    @Test
    void should_be_thrown_EVCacheInvalidKeyException_when_key_contain_whitespace() {
        assertThatThrownBy(() -> cache.lookup(" ")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                   .hasMessage("Key must not be contain whitespace");
        assertThatThrownBy(() -> cache.lookup("a b")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                     .hasMessage("Key must not be contain whitespace");
        assertThatThrownBy(() -> cache.lookup("ab ")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                     .hasMessage("Key must not be contain whitespace");
        assertThatThrownBy(() -> cache.lookup(" ab")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                     .hasMessage("Key must not be contain whitespace");
    }

    @Test
    void should_be_thrown_EVCacheInvalidKeyException_when_key_contain_whitespace_with_deleteWhitespaceKey() {
        cache = new EVCacheImpl("name", source, true, 10, true);
        assertThatThrownBy(() -> cache.lookup("   ")).isExactlyInstanceOf(EVCacheInvalidKeyException.class)
                                                     .hasMessage("Deleted whitespace key is empty");
    }

    @Test
    void should_be_lookup_when_key_contain_whitespace_with_deleteWhitespaceKey() throws EVCacheException {
        cache = new EVCacheImpl("name", source, true, 10, true);
        doReturn(1).when(source).get("ab");
        assertThat(cache.lookup("ab ")).isEqualTo(1);
        assertThat(cache.lookup("a   b")).isEqualTo(1);
        assertThat(cache.lookup("  ab")).isEqualTo(1);
        assertThat(cache.lookup("  a b ")).isEqualTo(1);
        verify(source, times(4)).get("ab");
    }
}
