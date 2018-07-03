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

package com.github.aafwu00.evcache.client.spring.boot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.aafwu00.evcache.client.spring.EVCache;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class EVCacheMeterBinderProviderTest {
    private EVCacheMeterBinderProvider provider;

    @BeforeEach
    void setUp() {
        provider = new EVCacheMeterBinderProvider();
    }

    @Test
    void getMeterBinder() {
        final EVCache cache = mock(EVCache.class);
        doReturn("name").when(cache).getName();
        doReturn("appName").when(cache).getAppName();
        doReturn("prefix").when(cache).getCachePrefix();
        assertThat(provider.getMeterBinder(cache, emptyList())).isNotNull();
    }
}
