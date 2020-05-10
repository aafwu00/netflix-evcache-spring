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

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.evcache.EVCache;
import com.netflix.evcache.EVCacheImpl;
import com.netflix.evcache.util.EVCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class EVCacheManagerTest {
    @Test
    void loadCaches() {
        final EVCacheConfiguration configuration1 = new EVCacheConfiguration("1",
                                                                             1,
                                                                             "TEST",
                                                                             "test1",
                                                                             Duration.ofSeconds(1000),
                                                                             true,
                                                                             true);
        final EVCacheConfiguration configuration2 = new EVCacheConfiguration("2",
                                                                             2,
                                                                             "TEST",
                                                                             "test2",
                                                                             Duration.ofSeconds(90),
                                                                             false,
                                                                             false);
        final Set<EVCacheConfiguration> configurations = new HashSet<>();
        configurations.add(configuration1);
        configurations.add(configuration2);
        final EVCache.Builder.Customizer customizer = mock(EVCache.Builder.Customizer.class);
        new EVCacheConfig(DefaultPropertyFactory.from(EmptyConfig.INSTANCE));
        final EVCacheManager manager = new EVCacheManager(configurations, singletonList(customizer));
        final List<? extends Cache> caches = new ArrayList<>(manager.loadCaches());
        assertThatCache(getNativeCache(caches, 0), configuration1);
        assertThatCache(getNativeCache(caches, 1), configuration2);
        verify(customizer, times(2)).customize(eq("TEST"), any());
    }

    private EVCacheImpl getNativeCache(final List<? extends Cache> caches, final int index) {
        return (EVCacheImpl) caches.get(index).getNativeCache();
    }

    private void assertThatCache(final EVCacheImpl cache, final EVCacheConfiguration configuration) {
        assertThat(cache.getAppName()).isEqualTo("TEST");
        assertThat(cache.getCachePrefix()).isEqualTo(configuration.getProperties().getKeyPrefix());
        assertThat(cache.getDefaultTTL()).isEqualTo(configuration.getProperties()
                                                                 .getTimeToLive()
                                                                 .getSeconds());
    }
}
