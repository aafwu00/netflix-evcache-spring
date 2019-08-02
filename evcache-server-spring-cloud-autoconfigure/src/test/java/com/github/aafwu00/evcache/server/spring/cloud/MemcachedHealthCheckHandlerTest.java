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
import org.springframework.boot.actuate.health.Health;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.DOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class MemcachedHealthCheckHandlerTest {
    private MemcachedHealthIndicator indicator;
    private MemcachedHealthCheckHandler handler;

    @BeforeEach
    void setUp() {
        indicator = mock(MemcachedHealthIndicator.class);
        handler = new MemcachedHealthCheckHandler(indicator);
    }

    @Test
    void up() throws Exception {
        doAnswer(invocation -> {
            final Health.Builder builder = invocation.getArgument(0);
            builder.up();
            return null;
        }).when(indicator).doHealthCheck(any());
        assertThat(handler.getStatus(null)).isEqualTo(UP);
    }

    @Test
    void down() throws Exception {
        doAnswer(invocation -> {
            final Health.Builder builder = invocation.getArgument(0);
            builder.down();
            return null;
        }).when(indicator).doHealthCheck(any());
        assertThat(handler.getStatus(null)).isEqualTo(DOWN);
    }

    @Test
    void outOfService() throws Exception {
        doAnswer(invocation -> {
            final Health.Builder builder = invocation.getArgument(0);
            builder.outOfService();
            return null;
        }).when(indicator).doHealthCheck(any());
        assertThat(handler.getStatus(null)).isEqualTo(OUT_OF_SERVICE);
    }

    @Test
    void unknown() throws Exception {
        doAnswer(invocation -> {
            final Health.Builder builder = invocation.getArgument(0);
            builder.unknown();
            return null;
        }).when(indicator).doHealthCheck(any());
        assertThat(handler.getStatus(null)).isEqualTo(UNKNOWN);
    }
}
