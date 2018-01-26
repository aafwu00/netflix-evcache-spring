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

package com.github.aafwu00.spring.netflix.evcache.client;

public class EVCachePutException extends EVCacheException {
    private static final long serialVersionUID = -6949955724904893839L;

    public EVCachePutException(final Object key, final Object value, final Throwable cause) {
        super("Put Error, Key:" + key + ", value:" + value, cause);
    }
}
