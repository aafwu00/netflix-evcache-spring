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

package com.github.aafwu00.evcache.spring;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.core.convert.ConversionService;

import com.netflix.evcache.EVCache.Builder;

import static java.util.Objects.requireNonNull;

/**
 * {@link CacheManager} backed by an {@link EVCache}.
 *
 * @author Taeho Kim
 */
public class EVCacheManager extends AbstractCacheManager {
    private final String clusterName;
    private final ConversionService conversionService;
    private final List<EVCacheConfiguration> configurations;
    private EVCachePostConstructCustomizer customizer = cache -> cache;

    public EVCacheManager(final String clusterName,
                          final ConversionService conversionService,
                          final List<EVCacheConfiguration> configurations) {
        super();
        this.clusterName = requireNonNull(clusterName);
        this.conversionService = requireNonNull(conversionService);
        this.configurations = requireNonNull(configurations);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return configurations.stream()
                             .map(this::create)
                             .map(customizer::customize)
                             .collect(Collectors.toList());
    }

    private EVCache create(final EVCacheConfiguration configuration) {
        return new EVCache(builder(configuration).build(),
                           conversionService,
                           configuration.isAllowNullValues(),
                           configuration.isKeyHash());
    }

    private Builder builder(final EVCacheConfiguration configuration) {
        final Builder builder = new Builder().setAppName(clusterName)
                                             .setCachePrefix(configuration.getName())
                                             .setDefaultTTL(configuration.getTimeToLive());
        if (configuration.isServerGroupRetry()) {
            builder.enableRetry();
        } else {
            builder.disableRetry();
        }
        if (configuration.isEnableExceptionThrowing()) {
            builder.enableExceptionPropagation();
        }
        return builder;
    }

    public void setCustomizer(final EVCachePostConstructCustomizer customizer) {
        this.customizer = requireNonNull(customizer);
    }
}
