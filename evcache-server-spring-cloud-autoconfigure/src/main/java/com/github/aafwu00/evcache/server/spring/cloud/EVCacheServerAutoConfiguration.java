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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * EVCache Server Configuration look on {@code org.springframework.cloud.netflix.sidecar.SidecarConfiguration}
 *
 * @author Taeho Kim
 */
@Configuration
@ConditionalOnBean(EVCacheServerMarkerConfiguration.Marker.class)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class EVCacheServerAutoConfiguration {
    @Bean
    public HasFeatures evcacheServerFeature() {
        return HasFeatures.namedFeature("EVCache Server", EVCacheServerAutoConfiguration.class);
    }

    @Bean
    public EVCacheServerEurekaInstanceConfigBeanPostProcessor evcacheServerEurekaInstanceConfigBeanPostProcessor(
        final ConfigurableEnvironment environment,
        final EurekaClientConfigBean eurekaClientConfigBean) {
        return new EVCacheServerEurekaInstanceConfigBeanPostProcessor(environment, eurekaClientConfigBean);
    }
}
