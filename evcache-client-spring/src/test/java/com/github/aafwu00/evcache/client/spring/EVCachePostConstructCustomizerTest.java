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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCachePostConstructCustomizerTest {
    @Test
    void compareTo() {
        final EVCachePostConstructCustomizer customizer1 = create(1);
        final EVCachePostConstructCustomizer customizer2 = create(2);
        assertAll(
            () -> {
                final List<EVCachePostConstructCustomizer> customizers = Arrays.asList(customizer1, customizer2);
                Collections.sort(customizers);
                assertThat(customizers).containsExactly(customizer1, customizer2);
            },
            () -> {
                final List<EVCachePostConstructCustomizer> customizers = Arrays.asList(customizer2, customizer1);
                Collections.sort(customizers);
                assertThat(customizers).containsExactly(customizer1, customizer2);
            }
        );
    }

    @Test
    void order() {
        final EVCachePostConstructCustomizer customizer = cache -> cache;
        assertThat(customizer.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @NotNull
    private EVCachePostConstructCustomizer create(final int order) {
        return new EVCachePostConstructCustomizer() {
            @Override
            public EVCache customize(EVCache cache) {
                return null;
            }

            @Override
            public int getOrder() {
                return order;
            }
        };
    }
}
//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme
