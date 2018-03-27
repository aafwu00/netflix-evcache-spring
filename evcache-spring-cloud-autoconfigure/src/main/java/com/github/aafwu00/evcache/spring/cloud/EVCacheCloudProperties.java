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

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClient;
import com.netflix.evcache.pool.EVCacheNodeList;
import com.netflix.evcache.pool.SimpleNodeListProvider;

/**
 * Configuration properties for the EVCache Cloud.
 *
 * @author Taeho Kim
 */
@Validated
@ConfigurationProperties(prefix = "evcache.cloud")
public class EVCacheCloudProperties {
    /**
     * Enable EVCache
     */
    private boolean enabled = true;
    /**
     * Mode for {@link EVCacheNodeList}
     */
    @NotNull
    private NodeListMode nodeListMode = NodeListMode.DataCenterAware;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public NodeListMode getNodeListMode() {
        return nodeListMode;
    }

    public void setNodeListMode(final NodeListMode nodeListMode) {
        this.nodeListMode = nodeListMode;
    }

    public EVCacheNodeList createEVCacheNodeList(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
        return nodeListMode.create(applicationInfoManager, eurekaClient);
    }

    public enum NodeListMode implements EvcacheNodeListFactory {
        DataCenterAware {
            @Override
            public EVCacheNodeList create(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
                return new DataCenterAwareEurekaNodeListProvider(applicationInfoManager, eurekaClient);
            }
        },
        Amazon {
            @Override
            public EVCacheNodeList create(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
                return new AmazonEurekaNodeListProvider(applicationInfoManager, eurekaClient);
            }
        },
        MyOwn {
            @Override
            public EVCacheNodeList create(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
                return new MyOwnEurekaNodeListProvider(applicationInfoManager, eurekaClient);
            }
        },
        Simple {
            @Override
            public EVCacheNodeList create(final ApplicationInfoManager applicationInfoManager, final EurekaClient eurekaClient) {
                return new SimpleNodeListProvider();
            }
        };
    }

    interface EvcacheNodeListFactory {
        EVCacheNodeList create(ApplicationInfoManager applicationInfoManager, EurekaClient eurekaClient);
    }
}

