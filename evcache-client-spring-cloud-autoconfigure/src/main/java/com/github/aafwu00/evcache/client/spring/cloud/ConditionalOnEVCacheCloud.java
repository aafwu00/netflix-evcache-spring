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

package com.github.aafwu00.evcache.client.spring.cloud;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Conditional} that checks whether or not a default info contributor is enabled.
 * Matches if the value of {@code evcache.cloud.enabled} property is {@code true}.
 * And, The value of {@code evcache.use.simple.node.list.provider} property is not configured or false.
 *
 * @author Taeho Kim
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional({ConditionalOnEVCacheCloud.OnEVCacheCloudPropertyCondition.class,
              ConditionalOnEVCacheCloud.OnEVCacheBeanCondition.class})
public @interface ConditionalOnEVCacheCloud {
    class OnEVCacheBeanCondition extends AllNestedConditions {
        OnEVCacheBeanCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(ApplicationInfoManager.class)
        static class ApplicationInfoManagerSupportBean {
        }

        @ConditionalOnBean(EurekaClient.class)
        static class EurekaClientSupportBean {
        }
    }

    class OnEVCacheCloudPropertyCondition extends AllNestedConditions {
        OnEVCacheCloudPropertyCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(value = "evcache.cloud.enabled", matchIfMissing = true)
        static class OnEVCacheCloudEnabled {
        }

        @ConditionalOnProperty(value = "evcache.use.simple.node.list.provider",
                               havingValue = "false",
                               matchIfMissing = true)
        static class OnEVCacheUseSimpleNodeListProviderDisabled {
        }
    }
}
