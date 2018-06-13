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

package com.github.aafwu00.evcache.server.spring.cloud;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.spy.memcached.MemcachedClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class MemcachedMetricsTest {
    private MemcachedClient client;
    private MemcachedMetrics metrics;

    @BeforeEach
    void setUp() {
        client = mock(MemcachedClient.class);
        metrics = new MemcachedMetrics(client);
    }

    @Test
    void empty_status() {
        doReturn(new HashMap<>()).when(client).getStats();
        assertThat(metrics.metrics()).isEmpty();
    }

    @Test
    void metrics() {
        final Map<String, String> values = new HashMap<>();
        values.put("pid", "1");
        values.put("free_space", "2");
        values.put("total_items:test", "3.0");
        values.put("test3", "3.0");
        values.put("free_chunks", "not number");
        final Map<SocketAddress, Map<String, String>> stats = new HashMap<>();
        stats.put(mock(SocketAddress.class), values);
        doReturn(stats).when(client).getStats();
        final Map<String, Number> result = new HashMap<>();
        metrics.metrics().forEach(x -> result.put(x.getName(), x.getValue()));
        assertThat(result).containsEntry("memcached.free_space", 2)
                          .containsEntry("memcached.total_items", 3.0F)
                          .doesNotContainKeys("memcached.test3", "memcached.pid");
    }
}
