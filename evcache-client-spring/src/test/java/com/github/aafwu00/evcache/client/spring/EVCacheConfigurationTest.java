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
            () -> assertThatThrownBy(() -> create("TEST", "", 10)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> create("TEST", "test:1", 10)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> create("TEST", "test1:", 10)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThat(create("TEST", "test-1", 10)).isNotNull()
        );
    }

    private EVCacheConfiguration create(String appName, String cachePrefix, int timeToLive) {
        return new EVCacheConfiguration(appName, cachePrefix, timeToLive, true, true, true);
    }

    @Test
    void should_valid_time_to_live() {
        assertAll(
            () -> assertThatThrownBy(() -> create("TEST", "test1", -1)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> create("TEST", "test1", 0)).isInstanceOf(IllegalArgumentException.class),
            () -> assertThat(create("TEST", "test1", 10)).isNotNull()
        );
    }

    @Test
    void should_be_equals_when_appName_and_cachePrefix_are_equals() {
        final EVCacheConfiguration config = create("TEST", "test1", 1);
        assertAll(
            () -> assertThat(config).isEqualTo(config),
            () -> assertThat(config).isEqualTo(create("TEST", "test1", 1)),
            () -> assertThat(config).isNotEqualTo(null),
            () -> assertThat(config).isNotEqualTo(1),
            () -> assertThat(config).isNotEqualTo(create("TEST1", "test1", 1)),
            () -> assertThat(config).isNotEqualTo(create("TEST", "test", 1))
        );
    }

    @Test
    void should_be_same_hashCode_when_appName_and_cachePrefix_are_equals() {
        final EVCacheConfiguration config = create("TEST", "test1", 1);
        assertAll(
            () -> assertThat(config).hasSameHashCodeAs(create("TEST", "test1", 1)),
            () -> assertThat(config.hashCode()).isNotEqualTo(create("TEST1", "test1", 1).hashCode()),
            () -> assertThat(config.hashCode()).isNotEqualTo(create("TEST", "test", 1).hashCode())
        );
    }
}
