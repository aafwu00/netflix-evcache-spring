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
import java.util.Map;

import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;

import com.netflix.evcache.metrics.EVCacheMetricsFactory;

import static java.util.stream.Collectors.toList;

/**
 * A {@link PublicMetrics} implementation that provides EVCache statistics.
 *
 * @author Taeho Kim
 */
public class EVCacheMetrics implements PublicMetrics {
    @Override
    public Collection<Metric<?>> metrics() {
        return allMonitor().entrySet()
                           .stream()
                           .map(entry -> new Metric<>("evcache." + entry.getKey(), entry.getValue()))
                           .collect(toList());
    }

    private Map<String, Number> allMonitor() {
        // CHECKSTYLE:OFF
        // FIXME: Is right?
        // CHECKSTYLE:ON
        return EVCacheMetricsFactory.getInstance().getAllMonitor();
    }
}
