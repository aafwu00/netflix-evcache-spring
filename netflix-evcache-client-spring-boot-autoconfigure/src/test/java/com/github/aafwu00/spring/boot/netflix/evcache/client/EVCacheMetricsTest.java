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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DefaultRegistry;

import static com.netflix.evcache.metrics.EVCacheMetricsFactory.ATTEMPT;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.INTERNAL;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.IN_MEMORY;
import static com.netflix.evcache.metrics.EVCacheMetricsFactory.METRIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCacheMetricsTest {
    private EVCacheMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new EVCacheMetrics();
        counters().clear();
    }

    @AfterEach
    void tearDown() {
        counters().clear();
    }

    @Test
    void metrics() {
        final Counter counter1 = new DefaultRegistry().counter(IN_MEMORY, METRIC, "hit");
        final Counter counter2 = new DefaultRegistry().counter(INTERNAL, METRIC, "load");
        final Counter counter3 = new DefaultRegistry().counter(IN_MEMORY, ATTEMPT, "do-not-contain");
        counter1.increment(1);
        counter2.increment(2);
        counter3.increment(3);
        counters().put("test1", counter1);
        counters().put("test2", counter2);
        counters().put("test3", counter3);
        assertAll(
            () -> assertThat(hasMetric("evcache.client.inmemorycache.hit", 1L)).isTrue(),
            () -> assertThat(hasMetric("internal-evc.client.load", 2L)).isTrue(),
            () -> assertThat(hasMetric("evcache.client.inmemorycache.do-not-contain", 3L)).isFalse()
        );
    }

    private boolean hasMetric(final String key, final Number value) {
        return metrics.metrics()
                      .stream()
                      .anyMatch(metric -> StringUtils.equals(key, metric.getName()) && metric.getValue().equals(value));
    }

    private Map<String, Counter> counters() {
        return EVCacheMetricsFactory.getInstance().getAllCounters();
    }
}
