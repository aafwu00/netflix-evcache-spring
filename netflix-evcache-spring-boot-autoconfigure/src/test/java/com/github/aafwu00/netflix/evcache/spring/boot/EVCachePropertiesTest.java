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

package com.github.aafwu00.netflix.evcache.spring.boot;

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

import com.github.aafwu00.netflix.evcache.spring.EVCacheConfiguration;

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
            () -> assertThat(properties.isEnabled()).isTrue(),
            () -> assertThat(properties.getName()).isEqualTo("test"),
            () -> assertThat(properties.getPrefixes().get(0).getName()).isEqualTo("test1"),
            () -> assertThat(properties.getPrefixes().get(0).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(properties.getPrefixes().get(0).isServerGroupRetry()).isTrue(),
            () -> assertThat(properties.getPrefixes().get(0).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(properties.getPrefixes().get(0).isKeyHash()).isTrue(),
            () -> assertThat(properties.getPrefixes().get(0).isAllowNullValues()).isTrue(),
            () -> assertThat(properties.getPrefixes().get(1).getName()).isEqualTo("test2"),
            () -> assertThat(properties.getPrefixes().get(1).isServerGroupRetry()).isFalse(),
            () -> assertThat(properties.getPrefixes().get(1).isAllowNullValues()).isTrue(),
            () -> assertThat(properties.getPrefixes().get(1).isKeyHash()).isFalse()
        );
    }

    @Test
    void should_be_converted_to_configurations() {
        final List<EVCacheConfiguration> configurations = properties.toConfigurations();
        assertAll(
            () -> assertThat(configurations.get(0).getName()).isEqualTo("test1"),
            () -> assertThat(configurations.get(0).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(configurations.get(0).isServerGroupRetry()).isTrue(),
            () -> assertThat(configurations.get(0).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(configurations.get(0).isAllowNullValues()).isTrue(),
            () -> assertThat(configurations.get(0).isKeyHash()).isTrue(),
            () -> assertThat(configurations.get(1).getName()).isEqualTo("test2"),
            () -> assertThat(configurations.get(1).isEnableExceptionThrowing()).isFalse(),
            () -> assertThat(configurations.get(1).isAllowNullValues()).isTrue(),
            () -> assertThat(configurations.get(1).isKeyHash()).isFalse()
        );
    }

    @Configuration
    @EnableConfigurationProperties(EVCacheProperties.class)
    static class Config {
    }
}
