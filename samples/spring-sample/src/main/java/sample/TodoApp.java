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

package sample;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.evcache.client.spring.EVCacheConfiguration;
import com.github.aafwu00.evcache.client.spring.EVCacheManager;
import com.netflix.evcache.util.EVCacheConfig;

import sample.repository.TodoRepository;

/**
 * @author Taeho Kim
 */
@Configuration
@ComponentScan
@EnableCaching
public class TodoApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoApp.class);

    public static void main(final String[] args) {
        final ApplicationContext context = new AnnotationConfigApplicationContext(TodoApp.class);
        final TodoRepository repository = context.getBean(TodoRepository.class);
        LOGGER.info("{}", repository.findAll());
        LOGGER.info("{}", repository.findAll());
    }

    @Bean
    public CacheManager cacheManager() {
        EVCacheConfig.getInstance()
                     .getDynamicStringProperty("TODO-NODES",
                                               "shard1=localhost:11211,localhost:11212;shard2=localhost:11213,localhost:11214")
                     .get();
        final EVCacheConfiguration configuration = new EVCacheConfiguration("todos", "TODO", "todos", 10, true, true, true);
        return new EVCacheManager(Collections.singleton(configuration));
    }
}
