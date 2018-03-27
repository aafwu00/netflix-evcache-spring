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

package com.github.aafwu00.evcache.spring.cloud;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.eureka.EurekaNodeListProvider;

import static org.springframework.aop.framework.AopProxyUtils.getSingletonTarget;
import static org.springframework.aop.support.AopUtils.isAopProxy;

/**
 * {@link EurekaNodeListProvider} wrapping class, Eureka based for Amazon DataCenter.
 *
 * @author Taeho Kim
 * @see EurekaNodeListProvider
 */
public class AmazonEurekaNodeListProvider extends EurekaNodeListProvider {
    public AmazonEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager,
                                        final EurekaClient eurekaClient) {
        super(applicationInfoManager, discoveryClient(eurekaClient));
    }

    private static DiscoveryClient discoveryClient(final EurekaClient eurekaClient) {
        if (isAopProxy(eurekaClient)) {
            return DiscoveryClient.class.cast(getSingletonTarget(eurekaClient));
        }
        return DiscoveryClient.class.cast(eurekaClient);
    }
}
