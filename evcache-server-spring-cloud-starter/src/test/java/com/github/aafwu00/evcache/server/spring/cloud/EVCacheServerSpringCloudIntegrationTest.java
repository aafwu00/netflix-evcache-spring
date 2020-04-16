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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Testcontainers
@Tags({@Tag("integration"), @Tag("docker")})
class EVCacheServerSpringCloudIntegrationTest {
    @Container
    static final GenericContainer MEMCACHED = new GenericContainer<>("memcached:alpine").withExposedPorts(11211);
    @Container
    static final GenericContainer EUREKA = new GenericContainer<>("springcloud/eureka").withExposedPorts(8761);

    @Autowired
    private EurekaAutoServiceRegistration registration;
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private MemcachedHealthIndicator healthIndicator;

    @DynamicPropertySource
    static void eurekaProperties(final DynamicPropertyRegistry registry) {
        registry.add("eureka.instance.metadata-map.evcache.port", MEMCACHED::getFirstMappedPort);
        registry.add("eureka.client.serviceUrl.defaultZone",
                     () -> "http://" + EUREKA.getContainerIpAddress() + ":" + EUREKA.getFirstMappedPort() + "/eureka/");
    }

    @Test
    @Timeout(120)
    void up() throws InterruptedException {
        registration.start();
        while (isEmpty(discoveryClient.getInstances("evcache"))) {
            Thread.sleep(1000);
        }
        assertThat(healthIndicator.getHealth(false).getStatus()).isEqualTo(Status.UP);
        assertThat(discoveryClient.getInstances("evcache").get(0).getMetadata()).containsKey("evcache.port");
    }

    @SpringBootApplication
    @EnableEVCacheServer
    static class TodoApp {
    }

}
