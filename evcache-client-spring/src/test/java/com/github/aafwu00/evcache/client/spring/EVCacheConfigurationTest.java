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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCacheConfigurationTest {
    private EVCacheConfiguration create(final String name, final String appName, final String keyPrefix, final int timeToLive) {
        return new EVCacheConfiguration(name, appName, keyPrefix, Duration.ofSeconds(timeToLive), true, true);
    }

    @Test
    void should_be_equals_when_appName_and_keyPrefix_are_equals() {
        final EVCacheConfiguration config = create("TEST", "TEST", "test1", 1);
        assertAll(
            () -> assertThat(config).isEqualTo(config),
            () -> assertThat(config).isEqualTo(create("TEST", "TEST1", "test1", 1)),
            () -> assertThat(config).isEqualTo(create("TEST", "TEST", "test", 1)),
            () -> assertThat(config).isNotEqualTo(null),
            () -> assertThat(config).isNotEqualTo(1),
            () -> assertThat(config).isNotEqualTo(create("TEST1", "TEST1", "test1", 1)),
            () -> assertThat(config).isNotEqualTo(create("TEST1", "TEST", "test", 1))
        );
    }

    @Test
    void should_be_same_hashCode_when_appName_and_keyPrefix_are_equals() {
        final EVCacheConfiguration config = create("TEST", "TEST", "test1", 1);
        assertAll(
            () -> assertThat(config).hasSameHashCodeAs(create("TEST", "TEST1", "test1", 1)),
            () -> assertThat(config).hasSameHashCodeAs(create("TEST", "TEST", "test", 1)),
            () -> assertThat(config.hashCode()).isNotEqualTo(create("TEST1", "TEST1", "test1", 1).hashCode()),
            () -> assertThat(config.hashCode()).isNotEqualTo(create("TEST1", "TEST", "test", 1).hashCode())
        );
    }
}
