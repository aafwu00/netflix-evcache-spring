/*
 * Copyright 2017-2020 the original author or authors.
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

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Taeho Kim
 */
class MemcachedReactiveHealthIndicatorTest {
    private Health.Builder builder;
    private MemcachedClient client;
    private MemcachedReactiveHealthIndicator indicator;
    private OperationFuture<Boolean> setFuture;
    private GetFuture<String> getFuture;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        builder = new Health.Builder();
        client = mock(MemcachedClient.class);
        indicator = new MemcachedReactiveHealthIndicator(client);
        setFuture = mock(OperationFuture.class);
        getFuture = mock(GetFuture.class);
    }

    @Test
    void up() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(true).when(setFuture).get(5, TimeUnit.SECONDS);
        doReturn(getFuture).when(client).asyncGet("__com.netflix.evcache.server.healthcheck");
        doReturn("Greed is good!").when(getFuture).get(5, TimeUnit.SECONDS);
        StepVerifier.create(indicator.doHealthCheck(builder))
                    .expectNext(new Health.Builder().up().build())
                    .verifyComplete();
    }

    @Test
    void should_be_outOfService_when_set_operation_is_false() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(false).when(setFuture).get(5, TimeUnit.SECONDS);
        doReturn(getFuture).when(client).asyncGet("__com.netflix.evcache.server.healthcheck");
        doReturn("Greed is good!").when(getFuture).get(5, TimeUnit.SECONDS);
        StepVerifier.create(indicator.doHealthCheck(builder))
                    .expectNext(new Health.Builder().outOfService().build())
                    .verifyComplete();
    }

    @Test
    void should_be_outOfService_when_get_value_is_not_matched() throws Exception {
        doReturn(setFuture).when(client).set("__com.netflix.evcache.server.healthcheck", 300, "Greed is good!");
        doReturn(true).when(setFuture).get(5, TimeUnit.SECONDS);
        doReturn(getFuture).when(client).asyncGet("__com.netflix.evcache.server.healthcheck");
        doReturn("").when(getFuture).get(5, TimeUnit.SECONDS);
        StepVerifier.create(indicator.doHealthCheck(builder))
                    .expectNext(new Health.Builder().outOfService().build())
                    .verifyComplete();
    }
}
