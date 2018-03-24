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

package com.github.aafwu00.netflix.evcache.spring;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.core.convert.ConversionService;

import com.netflix.evcache.EVCacheImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheManagerTest {
    private ConversionService converterService;
    private EVCacheManager manager;
    private String clusterName;
    private EVCachePostConstructCustomizer customizer;

    @BeforeEach
    void setUp() {
        clusterName = "name";
        converterService = mock(ConversionService.class);
        customizer = spy(new EVCachePostConstructCustomizer() {
            @Override
            public Cache customize(EVCache cache) {
                return cache;
            }
        });
    }

    @Test
    void loadCaches() {
        final EVCacheConfiguration configuration1 = new EVCacheConfiguration("test1", 1000, true, true, true, false);
        final EVCacheConfiguration configuration2 = new EVCacheConfiguration("test2", 90, true, false, false, false);
        final List<EVCacheConfiguration> configurations = new ArrayList<>();
        configurations.add(configuration1);
        configurations.add(configuration2);
        manager = new EVCacheManager(clusterName, converterService, configurations);
        manager.setCustomizer(customizer);
        final List<? extends Cache> caches = new ArrayList<>(manager.loadCaches());
        assertAll(
            () -> assertThatCache(getNativeCache(caches, 0), configuration1),
            () -> assertThatCache(getNativeCache(caches, 1), configuration2),
            () -> verify(customizer).customize((EVCache) caches.get(0)),
            () -> verify(customizer).customize((EVCache) caches.get(1))
        );
    }

    private EVCacheImpl getNativeCache(final List<? extends Cache> caches, final int index) {
        return (EVCacheImpl) caches.get(index).getNativeCache();
    }

    private void assertThatCache(final EVCacheImpl cache, final EVCacheConfiguration configuration) {
        assertAll(
            () -> assertThat(cache.getAppName()).isEqualTo(clusterName.toUpperCase()),
            () -> assertThat(cache.getCachePrefix()).isEqualTo(configuration.getName()),
            () -> assertThat(cache.getDefaultTTL()).isEqualTo(configuration.getTimeToLive())
        );
    }
}
