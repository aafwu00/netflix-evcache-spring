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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cloud.netflix.archaius.ConfigurableEnvironmentConfiguration;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that checks whether or not a default info contributor is enabled.
 * Matches if the value of {@code evcache.enabled} property is {@code true}.
 * And, The value of {@code spring.cache.type} property is not configured.
 *
 * @author Taeho Kim
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional({ConditionalOnEVCache.OnEVCacheEnabledCondition.class,
              ConditionalOnEVCache.OnMissingSpringCacheTypeCondition.class})
public @interface ConditionalOnEVCache {
    class OnEVCacheEnabledCondition extends AllNestedConditions {
        OnEVCacheEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnClass(ConfigurableEnvironmentConfiguration.class)
        static class ConfigurableEnvironmentConfigurationClass {
        }

        @ConditionalOnBean(CacheAspectSupport.class)
        static class CacheAspectSupportBean {
        }

        @ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
        static class CacheManagerMissingBean {
        }

        @ConditionalOnProperty(value = "evcache.enabled", matchIfMissing = true)
        static class OnEVCacheEnabled {
        }
    }

    class OnMissingSpringCacheTypeCondition extends NoneNestedConditions {
        OnMissingSpringCacheTypeCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty("spring.cache.type")
        static class SpringCacheType {
        }
    }
}
