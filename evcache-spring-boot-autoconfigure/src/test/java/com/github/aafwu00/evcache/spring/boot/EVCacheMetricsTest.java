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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.evcache.EVCache.Call;
import com.netflix.evcache.metrics.EVCacheMetricsFactory;
import com.netflix.evcache.metrics.Stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCacheMetricsTest {
    private EVCacheMetrics metrics;
    private Stats stats;

    @BeforeEach
    void setUp() {
        metrics = new EVCacheMetrics();
        EVCacheMetricsFactory.getAllMetrics().clear();
        stats = EVCacheMetricsFactory.getStats("test", "prefix");
        stats.operationCompleted(Call.GET, 1);
        stats.operationCompleted(Call.GET_AND_TOUCH, 1);
        stats.operationCompleted(Call.SET, 1);
        stats.operationCompleted(Call.REPLACE, 1);
        stats.operationCompleted(Call.DELETE, 1);
        stats.operationCompleted(Call.BULK, 1);
        stats.operationCompleted(Call.APPEND_OR_ADD, 1);
        stats.operationCompleted(Call.ADD, 1);
        stats.operationCompleted(Call.APPEND, 1);
        stats.operationCompleted(Call.INCR, 1);
        stats.operationCompleted(Call.DECR, 1);
    }

    @AfterEach
    void tearDown() {
        EVCacheMetricsFactory.getAllMetrics().clear();
    }

    @Test
    void metrics() {
        assertAll(
            () -> assertThat(hasMetric("evcache.test:prefix.cache.hits")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.cache.miss")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.get.call")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.get.duration")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.set.call")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.bulk.hits")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.bulk.miss")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.bulk.call")).isTrue(),
            () -> assertThat(hasMetric("evcache.test:prefix.bulk.duration")).isTrue()
        );
    }

    private boolean hasMetric(final String key) {
        return metrics.metrics()
                      .stream()
                      .anyMatch(metric -> StringUtils.equals(key, metric.getName()));
    }
}
