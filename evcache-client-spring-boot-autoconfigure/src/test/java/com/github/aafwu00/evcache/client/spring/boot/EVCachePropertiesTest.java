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

package com.github.aafwu00.evcache.client.spring.boot;

import com.github.aafwu00.evcache.client.spring.EVCacheConfiguration;
import com.github.aafwu00.evcache.client.spring.boot.EVCacheProperties.Cluster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EVCachePropertiesTest.Config.class,
                      initializers = ConfigFileApplicationContextInitializer.class)
@DirtiesContext
class EVCachePropertiesTest {
    @Autowired
    private EVCacheProperties properties;

    @Test
    void should_be_equal_to_clusterAppName_when_keyPrefix_is_not_exists() {
        assertThat(cluster("TEST").determineName()).isEqualTo("TEST");
    }

    @Test
    void should_be_compose_of_cluster_appName_and_prefix_when_keyPrefix_is_exists() {
        assertThat(cluster("TEST", "prefix").determineName()).isEqualTo("TEST.prefix");
    }

    @Test
    void should_be_equals_when_appName_and_keyPrefix_are_equals() {
        final Cluster cluster = cluster("TEST", "test1");
        assertThat(cluster).isEqualTo(cluster("TEST", "test1"));
        assertThat(cluster).isNotEqualTo(cluster("TEST1", "test1"));
        assertThat(cluster).isNotEqualTo(cluster("TEST", "test"));
        assertThat(cluster("TEST")).isEqualTo(cluster("TEST"));
        assertThat(cluster("TEST")).isNotEqualTo(cluster("TEST1"));
    }

    @Test
    void should_be_same_hashCode_when_appName_and_keyPrefix_are_equals() {
        final Cluster cluster = cluster("TEST", "test1");
        assertThat(cluster).hasSameHashCodeAs(cluster("TEST", "test1"));
        assertThat(cluster.hashCode()).isNotEqualTo(cluster("TEST1", "test1").hashCode());
        assertThat(cluster.hashCode()).isNotEqualTo(cluster("TEST", "test").hashCode());
        assertThat(cluster("TEST")).hasSameHashCodeAs(cluster("TEST"));
        assertThat(cluster("TEST").hashCode()).isNotEqualTo(cluster("TEST1").hashCode());
    }

    private Cluster cluster(final String appName, final String keyPrefix) {
        return new Cluster(null, appName, keyPrefix, ofSeconds(1), false, false);
    }

    private Cluster cluster(final String name) {
        return new Cluster(name, "", "", ofSeconds(1), false, false);
    }

    @Test
    void should_be_loaded_yml() {
        assertThat(properties.isEnabled()).isFalse();
        assertThat(first(properties.getClusters()).determineName()).isEqualTo("test");
        assertThat(first(properties.getClusters()).getName()).isEqualTo("test");
        assertThat(first(properties.getClusters()).getAppName()).isEqualTo("test");
        assertThat(first(properties.getClusters()).getKeyPrefix()).isEmpty();
        assertThat(first(properties.getClusters()).getTimeToLive()).isEqualTo(ofSeconds(1000));
        assertThat(first(properties.getClusters()).isRetryEnabled()).isTrue();
        assertThat(first(properties.getClusters()).isExceptionThrowingEnabled()).isTrue();
        assertThat(second(properties.getClusters()).determineName()).isEqualTo("test.test2");
        assertThat(second(properties.getClusters()).getName()).isNull();
        assertThat(second(properties.getClusters()).getAppName()).isEqualTo("test");
        assertThat(second(properties.getClusters()).getKeyPrefix()).isEqualTo("test2");
        assertThat(second(properties.getClusters()).isRetryEnabled()).isFalse();
        assertThat(second(properties.getClusters()).isExceptionThrowingEnabled()).isFalse();
    }

    @Test
    void should_be_converted_to_configurations() {
        final List<EVCacheConfiguration> configurations = new ArrayList<>(properties.toConfigurations());
        assertThat(first(configurations).getProperties().getKeyPrefix()).isEmpty();
        assertThat(first(configurations).getProperties().getTimeToLive()).isEqualTo(ofSeconds(1000));
        assertThat(first(configurations).getProperties().getRetryEnabled()).isTrue();
        assertThat(first(configurations).getProperties().getExceptionThrowingEnabled()).isTrue();
        assertThat(second(configurations).getProperties().getKeyPrefix()).isEqualTo("test2");
        assertThat(second(configurations).getProperties().getRetryEnabled()).isFalse();
        assertThat(second(configurations).getProperties().getExceptionThrowingEnabled()).isFalse();
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
