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

import java.util.ArrayList;
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

import com.github.aafwu00.evcache.client.spring.EVCacheConfiguration;
import com.github.aafwu00.evcache.client.spring.boot.EVCacheProperties.Cluster;

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
    void should_be_equals_when_appName_and_cachePrefix_are_equals() {
        final Cluster cluster = cluster("TEST", "test1");
        assertAll(
            () -> assertThat(cluster).isEqualTo(cluster),
            () -> assertThat(cluster).isEqualTo(cluster("TEST", "test1")),
            () -> assertThat(cluster).isNotEqualTo(null),
            () -> assertThat(cluster).isNotEqualTo(1),
            () -> assertThat(cluster).isNotEqualTo(cluster("TEST1", "test1")),
            () -> assertThat(cluster).isNotEqualTo(cluster("TEST", "test"))
        );
    }

    @Test
    void should_be_same_hashCode_when_appName_and_cachePrefix_are_equals() {
        final Cluster cluster = cluster("TEST", "test1");
        assertAll(
            () -> assertThat(cluster).hasSameHashCodeAs(cluster("TEST", "test1")),
            () -> assertThat(cluster.hashCode()).isNotEqualTo(cluster("TEST1", "test1").hashCode()),
            () -> assertThat(cluster.hashCode()).isNotEqualTo(cluster("TEST", "test").hashCode())
        );
    }

    private Cluster cluster(final String appName, final String cachePrefix) {
        final Cluster result = new Cluster();
        result.setAppName(appName);
        result.setCachePrefix(cachePrefix);
        return result;
    }

    @Test
    void should_be_loaded_yml() {
        assertAll(
            () -> assertThat(properties.isEnabled()).isFalse(),
            () -> assertThat(first(properties.getClusters()).getAppName()).isEqualTo("test"),
            () -> assertThat(first(properties.getClusters()).getCachePrefix()).isEqualTo("test1"),
            () -> assertThat(first(properties.getClusters()).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(first(properties.getClusters()).isServerGroupRetry()).isTrue(),
            () -> assertThat(first(properties.getClusters()).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(first(properties.getClusters()).isAllowNullValues()).isTrue(),
            () -> assertThat(second(properties.getClusters()).getAppName()).isEqualTo("test"),
            () -> assertThat(second(properties.getClusters()).getCachePrefix()).isEqualTo("test2"),
            () -> assertThat(second(properties.getClusters()).isServerGroupRetry()).isFalse(),
            () -> assertThat(second(properties.getClusters()).isEnableExceptionThrowing()).isFalse(),
            () -> assertThat(second(properties.getClusters()).isAllowNullValues()).isFalse()
        );
    }

    @Test
    void should_be_converted_to_configurations() {
        final List<EVCacheConfiguration> configurations = new ArrayList<>(properties.toConfigurations());
        assertAll(
            () -> assertThat(first(configurations).getCachePrefix()).isEqualTo("test1"),
            () -> assertThat(first(configurations).getTimeToLive()).isEqualTo(1000),
            () -> assertThat(first(configurations).isServerGroupRetry()).isTrue(),
            () -> assertThat(first(configurations).isEnableExceptionThrowing()).isTrue(),
            () -> assertThat(first(configurations).isAllowNullValues()).isTrue(),
            () -> assertThat(second(configurations).getCachePrefix()).isEqualTo("test2"),
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
