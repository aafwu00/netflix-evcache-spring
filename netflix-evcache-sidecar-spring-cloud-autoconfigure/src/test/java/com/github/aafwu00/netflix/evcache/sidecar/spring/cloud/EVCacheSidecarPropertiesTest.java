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

package com.github.aafwu00.netflix.evcache.sidecar.spring.cloud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EVCacheSidecarPropertiesTest.Config.class, initializers = ConfigFileApplicationContextInitializer.class)
@DirtiesContext
class EVCacheSidecarPropertiesTest {
    @Autowired
    private EVCacheSidecarProperties properties;

    @Test
    void should_be_loaded_yml() {
        assertAll(
            () -> assertThat(properties.isEnabled()).isTrue(),
            () -> assertThat(properties.getGroup()).isEqualTo("test"),
            () -> assertThat(properties.getHostname()).isEqualTo("localhost"),
            () -> assertThat(properties.getPort()).isEqualTo(11211)
        );
    }

    @Configuration
    @EnableConfigurationProperties(EVCacheSidecarProperties.class)
    static class Config {
    }
}
