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

package com.github.aafwu00.evcache.client.spring;

import com.netflix.evcache.EVCache.Builder;
import com.netflix.evcache.pool.EVCacheClientPoolManager;
import net.spy.memcached.transcoders.Transcoder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link CacheManager} backed by an {@link EVCacheImpl}.
 *
 * @author Taeho Kim
 */
public class EVCacheManager extends AbstractCacheManager {
    private final EVCacheClientPoolManager evcacheClientPoolManager;
    private final Set<EVCacheConfiguration> configurations;
    private final List<Builder.Customizer> customizers;
    /**
     * Whether to allow for {@code null} values
     */
    private boolean allowNullValues = true;
    /**
     * Delete whitespace key. Careful, Both of 'ab' and 'a b' are same key
     */
    private boolean deleteWhitespaceKey;
    /**
     * The default {@link Transcoder} to be used for serializing and
     * de-serializing items in {@link com.netflix.evcache.EVCache}.
     */
    private Transcoder<? extends Object> transcoder;

    /**
     * Create a new EVCacheManager for the given configurations and customizers
     *
     * @param evcacheClientPoolManager To be applied by for {@link Builder}`s poolManager
     * @param configurations           To be applied by for {@link Builder}`s
     *                                 {@link com.netflix.evcache.EVCacheClientPoolConfigurationProperties}
     * @param customizers              To be applied by for {@link Builder}`s customizers
     */
    public EVCacheManager(final EVCacheClientPoolManager evcacheClientPoolManager,
                          final Set<EVCacheConfiguration> configurations,
                          final List<Builder.Customizer> customizers) {
        super();
        Assert.notNull(evcacheClientPoolManager, "`evcacheClientPoolManager` must not be null");
        Assert.notNull(configurations, "`configurations` must not be null");
        Assert.notEmpty(configurations, "`configurations` must not be empty");
        Assert.notNull(customizers, "`customizers` must not be null");
        this.evcacheClientPoolManager = evcacheClientPoolManager;
        this.configurations = configurations;
        this.customizers = customizers;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return configurations.stream()
                             .map(this::create)
                             .collect(Collectors.toList());
    }

    private EVCache create(final EVCacheConfiguration configuration) {
        return new EVCacheImpl(configuration.getCacheName(),
                               build(configuration),
                               allowNullValues,
                               configuration.getStriped(),
                               deleteWhitespaceKey);
    }

    private com.netflix.evcache.EVCache build(final EVCacheConfiguration configuration) {
        final Builder builder = Builder.forApp(configuration.getAppName());
        final Field field = ReflectionUtils.findField(Builder.class, "_poolManager");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, builder, evcacheClientPoolManager);
        return builder.withConfigurationProperties(configuration.getProperties())
                      .addCustomizers(customizers)
                      .setTranscoder(transcoder)
                      .build();
    }

    public void setAllowNullValues(final boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }

    public void setDeleteWhitespaceKey(final boolean deleteWhitespaceKey) {
        this.deleteWhitespaceKey = deleteWhitespaceKey;
    }

    public void setTranscoder(final Transcoder<? extends Object> transcoder) {
        this.transcoder = transcoder;
    }
}
