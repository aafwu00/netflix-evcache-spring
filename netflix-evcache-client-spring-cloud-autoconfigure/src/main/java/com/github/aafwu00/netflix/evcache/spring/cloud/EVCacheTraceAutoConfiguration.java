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

package com.github.aafwu00.netflix.evcache.spring.cloud;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.netflix.evcache.spring.boot.EVCacheAutoConfiguration;

/**
 * Spring configuration for configuring EVCache Trace defaults to be Sleuth based
 *
 * @author Taeho Kim
 * @see TraceAutoConfiguration
 * @see EVCacheAutoConfiguration
 */
@Configuration
@ConditionalOnBean({Tracer.class, ErrorParser.class})
@ConditionalOnProperty(value = "evcache.trace.enabled", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
@AutoConfigureBefore(EVCacheAutoConfiguration.class)
public class EVCacheTraceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public EVCacheManagerTraceCustomizer evcacheManagerSleuthCustomizer(final Tracer tracer, final ErrorParser errorParser) {
        return new EVCacheManagerTraceCustomizer(tracer, errorParser);
    }
}
