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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.metrics.Metric;

import com.netflix.evcache.metrics.EVCacheMetricsFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
class EVCacheMetricsTest {
    @BeforeEach
    void setUp() {
        allMonitor().clear();
    }

    @AfterEach
    void tearDown() {
        allMonitor().clear();
    }

    @Test
    void metrics() {
        allMonitor().put("test1", 1);
        allMonitor().put("test2", 1.5);
        final EVCacheMetrics metrics = new EVCacheMetrics();
        final Map<String, Number> result = new HashMap<>();
        for (final Metric<?> metric : metrics.metrics()) {
            result.put(metric.getName(), metric.getValue());
        }
        assertThat(result).containsEntry("evcache.test1", 1)
                          .containsEntry("evcache.test2", 1.5);
    }

    private Map<String, Number> allMonitor() {
        return EVCacheMetricsFactory.getInstance().getAllMonitor();
    }
}
