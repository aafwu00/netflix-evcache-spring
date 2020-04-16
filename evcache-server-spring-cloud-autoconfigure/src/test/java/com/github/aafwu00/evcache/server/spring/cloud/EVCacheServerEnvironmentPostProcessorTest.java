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

package com.github.aafwu00.evcache.server.spring.cloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
class EVCacheServerEnvironmentPostProcessorTest {
    private ConfigurableEnvironment environment;
    private EVCacheServerEnvironmentPostProcessor processor;

    @BeforeEach
    void setUp() {
        environment = new StandardEnvironment();
        processor = new EVCacheServerEnvironmentPostProcessor();
    }

    @Test
    void testGetOrder() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 1);
    }

    @Test
    void should_be_equal_default_when_not_exist_asg_name() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.asg-name")).isEqualTo("DEFAULT");
    }

    @Test
    void should_be_not_change_when_exist_asg_name() {
        environment.getPropertySources()
                   .addLast(new MapPropertySource("test", singletonMap("eureka.instance.asg-name", "asg")));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.asg-name")).isEqualTo("asg");
    }

    @Test
    void should_be_set_when_exist_asg_name() {
        environment.getPropertySources()
                   .addLast(new MapPropertySource("test", singletonMap("evcache.asg-name", "group")));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.asg-name")).isEqualTo("group");
    }

    @Test
    void should_be_not_set_when_not_exist_evcache_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.evcache.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_evcache_port() {
        environment.getPropertySources().addLast(new MapPropertySource("test", singletonMap("evcache.port", 11211)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.evcache.port")).isEqualTo("11211");
    }

    @Test
    void should_be_not_set_when_not_exist_evcache_secure_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.evcache.secure.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_evcache_secure_port() {
        environment.getPropertySources()
                   .addLast(new MapPropertySource("test", singletonMap("evcache.secure.port", 11411)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.evcache.secure.port")).isEqualTo("11411");
    }

    @Test
    void should_be_not_set_when_not_exist_rend_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.rend.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_rend_port() {
        environment.getPropertySources().addLast(new MapPropertySource("test", singletonMap("rend.port", 11211)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.rend.port")).isEqualTo("11211");
    }

    @Test
    void should_be_not_set_when_not_exist_rend_batch_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.rend.batch.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_rend_batch_port() {
        environment.getPropertySources().addLast(new MapPropertySource("test", singletonMap("rend.batch.port", 11211)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.rend.batch.port")).isEqualTo("11211");
    }

    @Test
    void should_be_not_set_when_not_exist_udsproxy_memcached_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.udsproxy.memcached.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_udsproxy_memcached_port() {
        environment.getPropertySources()
                   .addLast(new MapPropertySource("test", singletonMap("udsproxy.memcached.port", 11211)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.udsproxy.memcached.port")).isEqualTo("11211");
    }

    @Test
    void should_be_not_set_when_not_exist_udsproxy_memento_port() {
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.containsProperty("eureka.instance.metadata-map.udsproxy.memento.port")).isFalse();
    }

    @Test
    void should_be_set_when_exist_udsproxy_memento_port() {
        environment.getPropertySources()
                   .addLast(new MapPropertySource("test", singletonMap("udsproxy.memento.port", 11211)));
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("eureka.instance.metadata-map.udsproxy.memento.port")).isEqualTo("11211");
    }
}
