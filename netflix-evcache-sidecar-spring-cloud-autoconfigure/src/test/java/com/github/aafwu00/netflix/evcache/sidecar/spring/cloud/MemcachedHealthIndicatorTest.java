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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Taeho Kim
 */
class MemcachedHealthIndicatorTest {
    private Health.Builder builder;
    private MemcachedClient client;
    private MemcachedHealthIndicator indicator;
    private OperationFuture<Boolean> setFuture;
    private GetFuture<String> getFuture;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        builder = mock(Health.Builder.class);
        client = mock(MemcachedClient.class);
        indicator = new MemcachedHealthIndicator(client);
        setFuture = mock(OperationFuture.class);
        getFuture = mock(GetFuture.class);
    }

    @Test
    void up() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(true).when(setFuture).get();
        doReturn(getFuture).when(client).asyncGet("__com.netflix.evcache.server.healthcheck");
        doReturn("Greed is good!").when(getFuture).get(5, TimeUnit.SECONDS);
        indicator.doHealthCheck(builder);
        verify(builder).up();
    }

    @Test
    void should_not_be_up_when_set_is_false() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(false).when(setFuture).get();
        indicator.doHealthCheck(builder);
        verify(builder, never()).up();
    }

    @Test
    void should_not_be_up_when_not_matched_get_value() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(true).when(setFuture).get();
        doReturn(getFuture).when(client).asyncGet("__com.netflix.evcache.server.healthcheck");
        doReturn("").when(getFuture).get(5, TimeUnit.SECONDS);
        indicator.doHealthCheck(builder);
        verify(builder, never()).up();
    }
}
