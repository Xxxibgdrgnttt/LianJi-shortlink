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

package com.nageoffer.shortlink.project.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.monitor.ShortLinkMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

/**
 * 短链接监控状态保存消息队列生产者
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final ShortLinkMetrics shortLinkMetrics;

    @Value("${short-link.stats.mq.type:rocketmq}")
    private String messageQueueType;

    @Value("${short-link.stats.mq.topic:short-link-stats}")
    private String topic;

    @Value("${short-link.stats.mq.tag:access}")
    private String tag;

    @Value("${short-link.stats.mq.send-timeout:3000}")
    private long sendTimeout;

    /**
     * 发送延迟消费短链接统计
     */
    public void send(ShortLinkStatsRecordDTO statsRecord) {
        if (StrUtil.equalsIgnoreCase(messageQueueType, "redis-stream")) {
            Map<String, String> producerMap = Map.of("statsRecord", JSON.toJSONString(statsRecord));
            stringRedisTemplate.opsForStream().add(SHORT_LINK_STATS_STREAM_TOPIC_KEY, producerMap);
            return;
        }
        if (StrUtil.isBlank(statsRecord.getEventId())) {
            log.error("短链接统计事件缺少 eventId，fullShortUrl={}", statsRecord.getFullShortUrl());
            return;
        }
        String destination = StrUtil.isBlank(tag) ? topic : topic + ":" + tag;
        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(statsRecord))
                .setHeader(RocketMQHeaders.KEYS, statsRecord.getEventId())
                .build();
        try {
            rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("短链接统计事件发送成功，eventId={}, msgId={}",
                            statsRecord.getEventId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable throwable) {
                    shortLinkMetrics.recordMessageSendFailure("callback");
                    log.error("短链接统计事件发送失败，eventId={}, fullShortUrl={}",
                            statsRecord.getEventId(), statsRecord.getFullShortUrl(), throwable);
                }
            }, sendTimeout);
        } catch (Exception ex) {
            shortLinkMetrics.recordMessageSendFailure("submit");
            log.error("短链接统计事件提交异步发送失败，eventId={}, fullShortUrl={}",
                    statsRecord.getEventId(), statsRecord.getFullShortUrl(), ex);
        }
    }
}
