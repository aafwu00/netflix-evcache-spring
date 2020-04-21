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

package com.github.aafwu00.evcache.client.spring.cloud;

import brave.Tracing;
import com.github.aafwu00.evcache.client.spring.boot.EVCacheAutoConfiguration;
import com.netflix.evcache.EVCacheTracingEventListener;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Spring configuration for configuring EVCache Trace defaults to be Sleuth based
 *
 * @author Taeho Kim
 * @see TraceAutoConfiguration
 * @see EVCacheAutoConfiguration
 */
@Configuration
@ConditionalOnProperty(value = "evcache.trace.enabled", matchIfMissing = true)
@ConditionalOnClass(EVCacheTracingEventListener.class)
@ConditionalOnMissingBean(EVCacheTracingEventListener.class)
@ConditionalOnBean({Tracing.class, EVCacheClientPoolManager.class})
@AutoConfigureAfter({TraceAutoConfiguration.class, EVCacheAutoConfiguration.class})
public class EVCacheTraceAutoConfiguration implements InitializingBean {
    private final Tracing tracing;
    private final EVCacheClientPoolManager manager;

    /**
     * Default Constructor
     *
     * @param tracing must not be null
     * @param manager must not be null
     */
    public EVCacheTraceAutoConfiguration(final Tracing tracing, final EVCacheClientPoolManager manager) {
        Assert.notNull(tracing, "`tracing` must not be null");
        Assert.notNull(manager, "`manager` must not be null");
        this.tracing = tracing;
        this.manager = manager;
    }

    @Override
    public void afterPropertiesSet() {
        new EVCacheTracingEventListener(manager, tracing.tracer());
    }
}
