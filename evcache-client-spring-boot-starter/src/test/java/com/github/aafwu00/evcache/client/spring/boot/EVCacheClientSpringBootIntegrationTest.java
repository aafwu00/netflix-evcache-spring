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

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
@ContextConfiguration(initializers = EVCacheClientSpringBootIntegrationTest.Initializer.class)
@Tags({@Tag("integration"), @Tag("docker")})
class EVCacheClientSpringBootIntegrationTest {
    public static final GenericContainer MEMCACHED;

    static {
        MEMCACHED = new GenericContainer<>("memcached:alpine").withExposedPorts(11211);
        MEMCACHED.start();
    }

    @Autowired
    private TodoApp.TodoRepository repository;
    @Autowired
    private MemcachedClient client;

    @AfterAll
    static void afterAll() {
        MEMCACHED.stop();
    }

    @Test
    void cached() throws ExecutionException, InterruptedException {
        assertThat(repository.findAll()).isEqualTo(client.asyncGet("todos:findAll").get());
    }

    @SpringBootApplication
    @EnableCaching
    static class TodoApp {
        @Repository
        static class TodoRepository {
            @Cacheable(cacheNames = "TODO.todos", key = "'findAll'")
            public List<Todo> findAll() {
                return Arrays.asList(new Todo("first"), new Todo("second"));
            }
        }

        @Bean
        public MemcachedClientFactoryBean memcachedClient(final ConfigurableEnvironment environment) {
            final MemcachedClientFactoryBean bean = new MemcachedClientFactoryBean();
            final String nodes = environment.getProperty("TODO-NODES");
            bean.setServers(StringUtils.remove(nodes, "shard1="));
            return bean;
        }

        static class Todo implements Serializable {
            private static final long serialVersionUID = -632306557208393893L;
            private String title;

            Todo(final String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }

            @Override
            public boolean equals(final Object that) {
                if (this == that) {
                    return true;
                }
                if (that == null || getClass() != that.getClass()) {
                    return false;
                }
                Todo todo = (Todo) that;
                return Objects.equals(title, todo.title);
            }

            @Override
            public int hashCode() {
                return Objects.hash(title);
            }
        }
    }

    static class Initializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "TODO-NODES:shard1=" + MEMCACHED.getContainerIpAddress() + ":" + MEMCACHED.getFirstMappedPort()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
