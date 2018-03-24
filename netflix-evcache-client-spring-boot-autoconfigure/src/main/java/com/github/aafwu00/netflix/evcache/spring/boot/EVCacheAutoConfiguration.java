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

package com.github.aafwu00.netflix.evcache.spring.boot;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.ConversionService;

import com.github.aafwu00.netflix.evcache.spring.EVCacheManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the EVCache. Creates a
 * {@link CacheManager} if necessary when caching is enabled via {@link EnableCaching}.
 * <p>
 * Cache store can be auto-detected or specified explicitly via configuration.
 *
 * @author Taeho Kim
 * @see EnableCaching
 */
@Configuration
@ConditionalOnEVCache
@AutoConfigureAfter(ArchaiusAutoConfiguration.class)
@AutoConfigureBefore(CacheAutoConfiguration.class)
@EnableConfigurationProperties(EVCacheProperties.class)
@PropertySource("classpath:evcache/evcache.properties")
public class EVCacheAutoConfiguration {
    @Bean
    public HasFeatures evcacheClientFeature() {
        return HasFeatures.namedFeature("Netflix EVCache Client", EVCacheManager.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(final CacheManagerCustomizers customizers,
                                     final ConversionService conversionService,
                                     final EVCacheProperties properties) {
        return customizers.customize(new EVCacheManager(properties.getName(), conversionService, properties.toConfigurations()));
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManagerCustomizers cacheManagerCustomizers(final ObjectProvider<List<CacheManagerCustomizer<?>>> customizers) {
        return new CacheManagerCustomizers(customizers.getIfAvailable());
    }

    @Bean
    @ConditionalOnClass(PublicMetrics.class)
    @ConditionalOnProperty(value = "evcache.metrics.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public PublicMetrics evcacheMetrics() {
        return new EVCacheMetrics();
    }
}
