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

package com.github.aafwu00.spring.boot.netflix.evcache.client;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;

import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.Tag;

import static com.netflix.evcache.metrics.EVCacheMetricsFactory.METRIC;
import static java.util.stream.Collectors.toList;

/**
 * A {@link PublicMetrics} implementation that provides EVCache statistics.
 *
 * @author Taeho Kim
 */
public class EVCacheMetrics implements PublicMetrics {
    private final EVCacheMetricsFactory metricsFactory;

    public EVCacheMetrics() {
        Spectator.globalRegistry().add(new DefaultRegistry());
        metricsFactory = EVCacheMetricsFactory.getInstance();
    }

    @Override
    public Collection<Metric<?>> metrics() {
        return metricsFactory.getAllCounters()
                             .values()
                             .stream()
                             .filter(this::hasMetric)
                             .map(this::metric)
                             .collect(toList());
    }

    private boolean hasMetric(final Counter counter) {
        for (final Tag tag : counter.id().tags()) {
            if (StringUtils.equals(tag.key(), METRIC)) {
                return true;
            }
        }
        return false;
    }

    private Metric<Long> metric(final Counter counter) {
        return new Metric<>(key(counter), counter.count());
    }

    private String key(final Counter counter) {
        for (final Tag tag : counter.id().tags()) {
            if (StringUtils.equals(tag.key(), METRIC)) {
                return counter.id().name() + "." + tag.value();
            }
        }
        return counter.id().toString();
    }
}
