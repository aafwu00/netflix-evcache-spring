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

import org.springframework.beans.factory.InitializingBean;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.EVCacheServerGroupConfig;
import com.netflix.evcache.pool.ServerGroup;

import static com.netflix.appinfo.DataCenterInfo.Name.Amazon;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * {@link EVCacheNodeList} implementation defaults to be delegate for DataCenter.
 *
 * @author Taeho Kim
 * @see MyOwnEurekaNodeListProvider
 * @see AmazonEurekaNodeListProvider
 */
public class DataCenterAwareEurekaNodeListProvider implements EVCacheNodeList, InitializingBean {
    private final ApplicationInfoManager applicationInfoManager;
    private final EurekaClient eurekaClient;
    private EVCacheNodeList delegate;

    public DataCenterAwareEurekaNodeListProvider(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
        this.applicationInfoManager = requireNonNull(applicationInfoManager);
        this.eurekaClient = requireNonNull(eurekaClient);
    }

    @Override
    public Map<ServerGroup, EVCacheServerGroupConfig> discoverInstances(final String appName) throws IOException {
        return delegate.discoverInstances(appName);
    }

    @Override
    public void afterPropertiesSet() {
        if (isNull(delegate)) {
            delegate = createNodeListProvider();
        }
    }

    private EVCacheNodeList createNodeListProvider() {
        if (isAmazonDataCenter(applicationInfoManager.getInfo())) {
            return amazonEurekaNodeListProvider();
        }
        return myOwnEurekaNodeListProvider();
    }

    private boolean isAmazonDataCenter(final InstanceInfo instanceInfo) {
        return isNull(instanceInfo.getDataCenterInfo())
            || Amazon == instanceInfo.getDataCenterInfo().getName();
    }

    private AmazonEurekaNodeListProvider amazonEurekaNodeListProvider() {
        return new AmazonEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }

    private MyOwnEurekaNodeListProvider myOwnEurekaNodeListProvider() {
        return new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
    }
}
