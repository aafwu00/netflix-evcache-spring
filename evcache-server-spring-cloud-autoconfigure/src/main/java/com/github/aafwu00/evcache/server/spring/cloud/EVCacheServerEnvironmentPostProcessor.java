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

package com.github.aafwu00.evcache.server.spring.cloud;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * EVCache Server Configuration that setting up {@link com.netflix.appinfo.EurekaInstanceConfig}.
 *
 * @author Taeho Kim
 * @see com.netflix.evcache.pool.eureka.EurekaNodeListProvider
 */
class EVCacheServerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final Logger LOGGER = LoggerFactory.getLogger(EVCacheServerEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final Map<String, Object> source = new HashMap<>();
        if (!environment.containsProperty("eureka.instance.asg-name")) {
            final String asgName = environment.getProperty("evcache.asg-name", "DEFAULT");
            source.put("eureka.instance.asg-name", asgName);
            LOGGER.warn("`eureka.instance.asg-name` is Missing, set `{}`", asgName);
        }
        putMetadataIfAbsent(source, environment, "evcache.port");
        putMetadataIfAbsent(source, environment, "evcache.secure.port");
        putMetadataIfAbsent(source, environment, "rend.port");
        putMetadataIfAbsent(source, environment, "rend.batch.port");
        putMetadataIfAbsent(source, environment, "udsproxy.memcached.port");
        putMetadataIfAbsent(source, environment, "udsproxy.memento.port");
        environment.getPropertySources()
                   .addLast(new MapPropertySource("evcache-server", source));
    }

    private void putMetadataIfAbsent(final Map<String, Object> metadata, final ConfigurableEnvironment environment, final String key) {
        if (environment.containsProperty(key)) {
            final Integer value = environment.getProperty(key, Integer.class);
            metadata.putIfAbsent("eureka.instance.metadata-map." + key, value);
            LOGGER.info("set property `eureka.instance.metadata-map.{} = {}`", key, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
