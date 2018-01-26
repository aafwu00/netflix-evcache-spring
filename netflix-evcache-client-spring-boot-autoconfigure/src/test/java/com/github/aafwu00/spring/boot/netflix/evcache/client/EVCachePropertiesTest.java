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

package com.github.aafwu00.spring.boot.netflix.evcache.client;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.aafwu00.spring.netflix.evcache.client.EVCacheConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

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
        final EVCacheProperties result = new EVCacheProperties();
        result.setEnabled(true);
        result.setName("test");
        final EVCacheProperties.Prefix prefix1 = new EVCacheProperties.Prefix();
        prefix1.setName("test1");
        prefix1.setTimeToLive(1000);
        prefix1.setServerGroupRetry(true);
        prefix1.setEnableExceptionThrowing(true);
        final EVCacheProperties.Prefix prefix2 = new EVCacheProperties.Prefix();
        prefix2.setName("test2");
        prefix2.setServerGroupRetry(false);
        result.setPrefixes(Arrays.asList(prefix1, prefix2));
        assertThat(properties).isEqualTo(result);
    }

    @Test
    void should_be_converted_to_configurations() {
        final EVCacheConfiguration configuration1 = new EVCacheConfiguration("test1", 1000, true, true, true);
        final EVCacheConfiguration configuration2 = new EVCacheConfiguration("test2", 900, true, false, false);
        assertThat(properties.toConfigurations()).containsExactly(configuration1, configuration2);
    }

    @Configuration
    @EnableConfigurationProperties(EVCacheProperties.class)
    static class Config {
    }
}
