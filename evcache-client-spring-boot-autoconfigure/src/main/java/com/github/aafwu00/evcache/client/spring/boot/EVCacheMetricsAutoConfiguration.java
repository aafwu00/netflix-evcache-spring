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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.aafwu00.evcache.client.spring.EVCacheManager;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.micrometer.MicrometerRegistry;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Configuration
@ConditionalOnClass(MeterBinder.class)
@ConditionalOnBean(EVCacheManager.class)
@ConditionalOnProperty(value = "evcache.metrics.enabled", matchIfMissing = true)
@AutoConfigureAfter({EVCacheAutoConfiguration.class, MetricsAutoConfiguration.class})
public class EVCacheMetricsAutoConfiguration {
    @Bean
    public EVCacheMeterBinderProvider evcacheMeterBinderProvider() {
        return new EVCacheMeterBinderProvider();
    }

    @Configuration
    @ConditionalOnClass(MicrometerRegistry.class)
    public static class SpectatorRegistration {
        @Bean
        public static BeanPostProcessor micrometerRegistryBeanPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
                    return bean;
                }

                @Override
                public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
                    if (bean instanceof MeterRegistry) {
                        Spectator.globalRegistry()
                                 .add(new MicrometerRegistry((MeterRegistry) bean));
                    }
                    return bean;
                }
            };
        }
    }
}
