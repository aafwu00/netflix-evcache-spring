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

package com.github.aafwu00.netflix.evcache.sidecar.spring.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;

import net.spy.memcached.MemcachedClient;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.math.NumberUtils.createNumber;
import static org.apache.commons.lang3.math.NumberUtils.isNumber;

/**
 * A {@link PublicMetrics} implementation that provides Memcached statistics.
 *
 * @author Taeho Kim
 */
public class MemcachedMetrics implements PublicMetrics {
    private static final Set<String> GAUGE_NAMES = new HashSet<>(Arrays.asList("bytes",
                                                                               "free_space",
                                                                               "curr_items",
                                                                               "curr_connections",
                                                                               "chunk_size",
                                                                               "chunks_per_page",
                                                                               "total_pages",
                                                                               "total_chunks",
                                                                               "used_chunks",
                                                                               "free_chunks",
                                                                               "free_chunks_end",
                                                                               "mem_requested",
                                                                               "total_connections",
                                                                               "connection_structures",
                                                                               "limit_maxbytes",
                                                                               "total_items",
                                                                               "mem_requested",
                                                                               "active_slabs",
                                                                               "total_malloced",
                                                                               "slab_global_page_pool"));
    private final MemcachedClient client;

    public MemcachedMetrics(final MemcachedClient client) {
        super();
        this.client = requireNonNull(client);
    }

    @Override
    public Collection<Metric<?>> metrics() {
        final List<Metric<?>> result = new ArrayList<>();
        first().forEach((key, value) -> {
            final String name = substringBefore(key, ":");
            if (GAUGE_NAMES.contains(name) && isNumber(value)) {
                result.add(new Metric<>("memcached." + name, createNumber(value)));
            }
        });
        return result;
    }

    private Map<String, String> first() {
        return client.getStats().values().stream().findFirst().orElseGet(Collections::emptyMap);
    }
}
