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

package com.github.aafwu00.evcache.spring.boot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;

import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.evcache.metrics.Stats;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Spectator;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.ClassUtils.isAssignableValue;

/**
 * A {@link PublicMetrics} implementation that provides EVCache statistics.
 *
 * @author Taeho Kim
 */
public class EVCacheMetrics implements PublicMetrics {
    public EVCacheMetrics() {
        Spectator.globalRegistry().add(new DefaultRegistry());
    }

    @Override
    public Collection<Metric<?>> metrics() {
        return EVCacheMetricsFactory.getAllMetrics()
                                    .entrySet()
                                    .stream()
                                    .filter(this::isEVCacheMetrics)
                                    .map(this::toMetric)
                                    .flatMap(Collection::stream)
                                    .collect(toList());
    }

    private boolean isEVCacheMetrics(final Map.Entry<String, Stats> entry) {
        return isAssignableValue(com.netflix.evcache.metrics.EVCacheMetrics.class, entry.getValue());
    }

    private List<Metric<Number>> toMetric(final Map.Entry<String, Stats> entry) {
        final List<Metric<Number>> result = new ArrayList<>();
        final com.netflix.evcache.metrics.EVCacheMetrics state = com.netflix.evcache.metrics.EVCacheMetrics.class.cast(entry.getValue());
        add(result, entry.getKey(), "cache.hits", state::getCacheHits);
        add(result, entry.getKey(), "cache.miss", state::getCacheMiss);
        if (state.getGetCalls() != 0) {
            add(result, entry.getKey(), "hit.rate", state::getHitRate);
        }
        add(result, entry.getKey(), "get.call", state::getGetCalls);
        add(result, entry.getKey(), "get.duration", state::getGetDuration);
        add(result, entry.getKey(), "set.call", state::getSetCalls);
        add(result, entry.getKey(), "bulk.hits", state::getBulkHits);
        add(result, entry.getKey(), "bulk.miss", state::getBulkMiss);
        add(result, entry.getKey(), "bulk.call", state::getBulkCalls);
        if (state.getBulkCalls() != 0) {
            add(result, entry.getKey(), "bulk.hit.rate", state::getBulkHitRate);
        }
        add(result, entry.getKey(), "bulk.duration", state::getBulkDuration);
        return result;
    }

    private void add(final List<Metric<Number>> result, final String key, final String name, final Supplier<Number> value) {
        result.add(new Metric<>("evcache." + key + "." + name, value.get()));
    }
}
