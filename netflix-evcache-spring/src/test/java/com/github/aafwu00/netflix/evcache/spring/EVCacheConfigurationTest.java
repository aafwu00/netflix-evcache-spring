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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCacheConfigurationTest {
    @Test
    void should_valid_name() {
        assertAll(
            () -> assertThatThrownBy(() -> new EVCacheConfiguration("",
                                                                    10,
                                                                    true,
                                                                    true, true,
                                                                    false
            )).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> new EVCacheConfiguration("test:1",
                                                                    10,
                                                                    true,
                                                                    true, true,
                                                                    false
            )).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> new EVCacheConfiguration("test1:",
                                                                    10,
                                                                    true,
                                                                    true, true,
                                                                    false
            )).isInstanceOf(IllegalArgumentException.class),
            () -> assertThat(new EVCacheConfiguration("test-1", 10, true, true, true, false)).isNotNull()
        );
    }

    @Test
    void should_valid_time_to_live() {
        assertAll(
            () -> assertThatThrownBy(() -> new EVCacheConfiguration("test1",
                                                                    -1,
                                                                    true,
                                                                    true, true,
                                                                    false
            )).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> new EVCacheConfiguration("test1",
                                                                    0,
                                                                    true,
                                                                    true, true,
                                                                    false
            )).isInstanceOf(IllegalArgumentException.class),
            () -> assertThat(new EVCacheConfiguration("test1", 10, true, true, true, false)).isNotNull()
        );
    }
}
