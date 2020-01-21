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

package sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sample.repository.Todo;
import sample.repository.TodoRepository;

import java.util.List;

/**
 * @author Taeho Kim
 */
@SpringBootApplication
@ComponentScan(basePackageClasses = TodoRepository.class)
@EnableCaching
public class TodoBootApp {
    public static void main(final String[] args) {
        new SpringApplicationBuilder(TodoBootApp.class)
            .run(args);
    }

    @RestController
    static class TodoController {
        @Autowired
        private TodoRepository repository;

        @GetMapping("/todos")
        List<Todo> all() {
            return repository.findAll();
        }
    }
}
