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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.evcache.client.spring.boot.EVCacheAutoConfiguration;
import com.netflix.evcache.pool.EVCacheClientPoolManager;

import brave.Tracing;

import static java.util.Objects.requireNonNull;

/**
 * Spring configuration for configuring EVCache Trace defaults to be Sleuth based
 *
 * @author Taeho Kim
 * @see TraceAutoConfiguration
 * @see EVCacheAutoConfiguration
 */
@Configuration
@ConditionalOnProperty(value = "evcache.trace.enabled", matchIfMissing = true)
@ConditionalOnBean({Tracing.class, EVCacheClientPoolManager.class})
@AutoConfigureAfter({TraceAutoConfiguration.class, EVCacheAutoConfiguration.class})
public class EVCacheTraceAutoConfiguration implements InitializingBean {
    private final Tracing tracing;
    private final EVCacheClientPoolManager manager;

    public EVCacheTraceAutoConfiguration(final Tracing tracing, final EVCacheClientPoolManager manager) {
        this.tracing = requireNonNull(tracing);
        this.manager = requireNonNull(manager);
    }

    @Override
    public void afterPropertiesSet() {
        manager.addEVCacheEventListener(new TraceableListener(tracing));
    }
}
