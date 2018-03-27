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

import java.io.IOException;
import java.util.Map;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanCreationException;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;
import com.netflix.evcache.pool.eureka.EurekaNodeListProvider;

import static com.netflix.appinfo.DataCenterInfo.Name.Amazon;
import static java.util.Objects.isNull;
import static org.springframework.aop.support.AopUtils.isAopProxy;

public class DataCenterAwareEurekaNodeListProvider implements EVCacheNodeList {
    private final EVCacheNodeList delegate;

    public DataCenterAwareEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
        delegate = createNodeListProvider(applicationInfoManager, eurekaClient);
    }

    private EVCacheNodeList createNodeListProvider(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
        final InstanceInfo instanceInfo = applicationInfoManager.getInfo();
        if (isAmazonDataCenter(instanceInfo)) {
            return amazonEurekaNodeListProvider(applicationInfoManager, eurekaClient);
        }
        return myOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }

    private boolean isAmazonDataCenter(final InstanceInfo instanceInfo) {
        return isNull(instanceInfo.getDataCenterInfo())
            || Amazon == instanceInfo.getDataCenterInfo().getName();
    }

    private EurekaNodeListProvider amazonEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager,
                                                                final EurekaClient eurekaClient) {
        return new EurekaNodeListProvider(applicationInfoManager, casting(eurekaClient));
    }

    private DiscoveryClient casting(final EurekaClient eurekaClient) {
        if (isAopProxy(eurekaClient)) {
            try {
                return DiscoveryClient.class.cast(Advised.class.cast(eurekaClient).getTargetSource().getTarget());
                // CHECKSTYLE:OFF
            } catch (final Exception ex) {
                // CHECKSTYLE:ON
                throw new BeanCreationException("eurekaClient", "proxy casting exception", ex);
            }
        }
        return DiscoveryClient.class.cast(eurekaClient);
    }

    private MyOwnEurekaNodeListProvider myOwnEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager,
                                                                    final EurekaClient eurekaClient) {
        return new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }

    @Override
    public Map<ServerGroup, EVCacheServerGroupConfig> discoverInstances(final String appName) throws IOException {
        return delegate.discoverInstances(appName);
    }
}
