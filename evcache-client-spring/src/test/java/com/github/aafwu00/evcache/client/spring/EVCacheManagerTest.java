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

package com.github.aafwu00.evcache.client.spring;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import com.netflix.evcache.EVCacheImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheManagerTest {
    private EVCachePostConstructCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = spy(new EVCachePostConstructCustomizer() {
            @Override
            public EVCache customize(final EVCache cache) {
                return cache;
            }
        });
    }

    @Test
    void loadCaches() {
        final EVCacheConfiguration configuration1 = new EVCacheConfiguration("1", "TEST", "test1", Duration.ofSeconds(1000), true, true);
        final EVCacheConfiguration configuration2 = new EVCacheConfiguration("2", "TEST", "test2", Duration.ofSeconds(90), false, false);
        final Set<EVCacheConfiguration> configurations = new HashSet<>();
        configurations.add(configuration1);
        configurations.add(configuration2);
        final EVCacheManager manager = new EVCacheManager(configurations);
        manager.addCustomizer(customizer);
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
            () -> assertThat(cache.getAppName()).isEqualTo("TEST"),
            () -> assertThat(cache.getCachePrefix()).isEqualTo(configuration.getProperties().getKeyPrefix()),
            () -> assertThat(cache.getDefaultTTL()).isEqualTo(configuration.getProperties().getTimeToLive().getSeconds())
        );
    }
}
