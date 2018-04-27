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

package com.github.aafwu00.evcache.spring.boot;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.aafwu00.evcache.spring.EVCacheConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EVCachePropertiesTest.Config.class, initializers = ConfigFileApplicationContextInitializer.class)
@DirtiesContext
class EVCachePropertiesTest {
    @Autowired
    private EVCacheProperties properties;

    @Test
    void should_be_loaded_yml() {
        assertAll(
            () -> assertThat(properties.isEnabled()).isFalse(),
            () -> assertThat(properties.getName()).isEqualTo("test"),
            () -> assertThat(first(properties.getPrefixes()).getName()).isEqualTo("test1"),
            () -> assertThat(first(properties.getPrefixes()).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(first(properties.getPrefixes()).isServerGroupRetry()).isTrue(),
            () -> assertThat(first(properties.getPrefixes()).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(first(properties.getPrefixes()).isAllowNullValues()).isTrue(),
            () -> assertThat(second(properties.getPrefixes()).getName()).isEqualTo("test2"),
            () -> assertThat(second(properties.getPrefixes()).isServerGroupRetry()).isFalse(),
            () -> assertThat(second(properties.getPrefixes()).isEnableExceptionThrowing()).isFalse(),
            () -> assertThat(second(properties.getPrefixes()).isAllowNullValues()).isFalse()
        );
    }

    @Test
    void should_be_converted_to_configurations() {
        final List<EVCacheConfiguration> configurations = properties.toConfigurations();
        assertAll(
            () -> assertThat(first(configurations).getName()).isEqualTo("test1"),
            () -> assertThat(first(configurations).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(first(configurations).isServerGroupRetry()).isTrue(),
            () -> assertThat(first(configurations).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(first(configurations).isAllowNullValues()).isTrue(),
            () -> assertThat(second(configurations).getName()).isEqualTo("test2"),
            () -> assertThat(second(configurations).isServerGroupRetry()).isFalse(),
            () -> assertThat(second(configurations).isEnableExceptionThrowing()).isFalse(),
            () -> assertThat(second(configurations).isAllowNullValues()).isFalse()
        );
    }

    private <T> T first(final List<T> list) {
        return list.get(0);
    }

    private <T> T second(final List<T> list) {
        return list.get(1);
    }

    @Configuration
    @EnableConfigurationProperties(EVCacheProperties.class)
    static class Config {
    }
}
