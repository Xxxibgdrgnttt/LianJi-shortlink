/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.project.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 短链接核心链路指标。
 */
@Component
@RequiredArgsConstructor
public class ShortLinkMetrics {

    private final MeterRegistry meterRegistry;

    public void recordCacheHit(String cacheType) {
        Counter.builder("short.link.cache.hit")
                .description("Short link redirect requests served by cache")
                .tag("cache_type", cacheType)
                .register(meterRegistry)
                .increment();
    }

    public void recordDatabaseFallback(String result) {
        Counter.builder("short.link.database.fallback")
                .description("Requests that query the database after cache misses")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordMessageSendFailure(String stage) {
        Counter.builder("short.link.message.send.failure")
                .description("Short link statistics messages that fail to send")
                .tag("stage", stage)
                .register(meterRegistry)
                .increment();
    }

    public void recordDuplicateConsumption(String layer) {
        Counter.builder("short.link.mq.duplicate.consumption")
                .description("Duplicate or in-progress statistics message deliveries")
                .tag("layer", layer)
                .register(meterRegistry)
                .increment();
    }
}
