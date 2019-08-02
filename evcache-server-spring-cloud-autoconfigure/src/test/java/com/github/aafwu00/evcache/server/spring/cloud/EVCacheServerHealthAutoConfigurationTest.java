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

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Configuration;

import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;

import net.spy.memcached.MemcachedClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * @author Taeho Kim
 */
class EVCacheServerHealthAutoConfigurationTest {
    private static CouchbaseMock server;
    private ApplicationContextRunner contextRunner;

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        final BucketConfiguration config = new BucketConfiguration();
        config.bucketStartPort = 11211;
        config.numNodes = 1;
        config.type = Bucket.BucketType.MEMCACHED;
        config.name = "memcached";
        server = new CouchbaseMock(11210, Collections.singletonList(config));
        server.start();
        server.waitForStartup();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UtilAutoConfiguration.class,
                                                     EurekaDiscoveryClientConfiguration.class,
                                                     EurekaClientAutoConfiguration.class,
                                                     EVCacheServerAutoConfiguration.class,
                                                     EVCacheServerHealthAutoConfiguration.class
            )).withPropertyValues("spring.cloud.service-registry.auto-registration.enabled=false");
    }

    @Test
    void should_be_loaded_EnableEVCacheServerConfiguration() {
        contextRunner.withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertAll(
                         () -> assertThat(context.getBean(MemcachedClient.class)).isNotNull(),
                         () -> assertThat(context.getBean(MemcachedHealthCheckHandler.class)).isNotNull(),
                         () -> assertThat(context.getBean(MemcachedHealthIndicator.class)).isNotNull()
                     ));
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_EVCacheServer_health_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedClient.class));
    }

    @Test
    void should_be_not_loaded_MemcachedHealthCheckHandler_when_evcache_health_eureka_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.eureka.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedHealthCheckHandler.class));
    }

    @Test
    void should_be_not_loaded_MemcachedHealthIndicator_when_evcache_health_memcached_enabled_is_false() {
        contextRunner.withPropertyValues("evcache.server.health.memcached.enabled=false")
                     .withUserConfiguration(EnableEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedHealthIndicator.class));
    }

    @Test
    void should_be_not_loaded_MemcachedClient_when_not_exists_EnableEVCacheServer() {
        contextRunner.withUserConfiguration(NoEVCacheServerConfiguration.class)
                     .run(context -> assertThat(context).doesNotHaveBean(MemcachedClient.class));
    }

    @Configuration
    static class NoEVCacheServerConfiguration {
    }

    @Configuration
    @EnableEVCacheServer
    static class EnableEVCacheServerConfiguration {
    }
}
