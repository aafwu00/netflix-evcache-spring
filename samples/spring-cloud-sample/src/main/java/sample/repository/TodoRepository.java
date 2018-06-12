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

package sample.repository;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 * @author Taeho Kim
 */
@Repository
public class TodoRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoRepository.class);

    @Cacheable(cacheNames = "TODO.todos", key = "'findAll'")
    public List<Todo> findAll() {
        LOGGER.info("CALLED");
        return Arrays.asList(new Todo("first"), new Todo("second"));
    }
}
