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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;

import static com.couchbase.mock.memcached.Storage.StorageType.CACHE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Taeho Kim
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext
@Tag("integration")
class EVCacheSpringBootIntegrationTest {
    private static CouchbaseMock server;
    @Autowired
    private TodoApp.TodoRepository repository;

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

    @Test
    void cached() {
        repository.findAll();
        assertThat(server.getBuckets().get("memcached").getMasterItems(CACHE)).isNotEmpty();
        server.getBuckets().get("memcached").getMasterItems(CACHE)
              .forEach(item -> assertThat(item.getKeySpec().key).isEqualTo("todos:findAll"));
    }

    @SpringBootApplication
    @EnableCaching
    static class TodoApp {
        @Bean
        public ConversionService conversionService() {
            return DefaultConversionService.getSharedInstance();
        }

        @Repository
        static class TodoRepository {
            @Cacheable(cacheNames = "todos", key = "'findAll'")
            public List<Todo> findAll() {
                return Arrays.asList(new Todo("first"), new Todo("second"));
            }
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
        }
    }
}
