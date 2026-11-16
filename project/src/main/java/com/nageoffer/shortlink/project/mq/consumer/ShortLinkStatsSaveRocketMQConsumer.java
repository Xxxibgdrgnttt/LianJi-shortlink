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

package com.nageoffer.shortlink.project.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 短链接统计消费者。正常返回时确认消息，抛出异常时触发重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "short-link.stats.mq.type", havingValue = "rocketmq", matchIfMissing = true)
@RocketMQMessageListener(
        topic = "${short-link.stats.mq.topic:short-link-stats}",
        consumerGroup = "${short-link.stats.mq.consumer-group:short-link-stats-consumer}",
        selectorExpression = "${short-link.stats.mq.tag:access}",
        maxReconsumeTimes = 3
)
public class ShortLinkStatsSaveRocketMQConsumer implements RocketMQListener<String> {

    private final ShortLinkStatsPersistenceHandler statsPersistenceHandler;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Override
    public void onMessage(String message) {
        ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(message, ShortLinkStatsRecordDTO.class);
        String eventId = statsRecord == null ? null : statsRecord.getEventId();
        if (StrUtil.isBlank(eventId)) {
            throw new IllegalArgumentException("短链接统计消息缺少 eventId");
        }
        if (messageQueueIdempotentHandler.isMessageBeingConsumed(eventId)) {
            if (messageQueueIdempotentHandler.isAccomplish(eventId)) {
                return;
            }
            throw new ServiceException("统计事件尚未处理完成，触发 RocketMQ 重试");
        }
        try {
            boolean processed = statsPersistenceHandler.actualSaveShortLinkStats(statsRecord);
            messageQueueIdempotentHandler.setAccomplish(eventId);
            log.info("短链接统计事件消费成功，eventId={}, fullShortUrl={}, processed={}",
                    eventId, statsRecord.getFullShortUrl(), processed);
        } catch (Throwable ex) {
            messageQueueIdempotentHandler.delMessageProcessed(eventId);
            log.error("短链接统计事件消费失败，eventId={}, fullShortUrl={}",
                    eventId, statsRecord.getFullShortUrl(), ex);
            throw ex;
        }
    }
}
