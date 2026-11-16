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

package com.nageoffer.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkGroupStatsReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkStatsReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接监控控制层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@RestController(value = "shortLinkStatsControllerByAdmin")
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ObjectMapper objectMapper;
    private final com.nageoffer.shortlink.project.service.ShortLinkStatsService projectShortLinkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        var projectRequest = objectMapper.convertValue(
                requestParam,
                com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO.class
        );
        var projectStats = projectShortLinkStatsService.oneShortLinkStats(projectRequest);
        return Results.success(objectMapper.convertValue(projectStats, ShortLinkStatsRespDTO.class));
    }

    /**
     * 访问分组短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        var projectRequest = objectMapper.convertValue(
                requestParam,
                com.nageoffer.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO.class
        );
        var projectStats = projectShortLinkStatsService.groupShortLinkStats(projectRequest);
        return Results.success(objectMapper.convertValue(projectStats, ShortLinkStatsRespDTO.class));
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        var projectRequest = objectMapper.convertValue(
                requestParam,
                com.nageoffer.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO.class
        );
        var projectPage = projectShortLinkStatsService.shortLinkStatsAccessRecord(projectRequest);
        var resultPage = new Page<ShortLinkStatsAccessRecordRespDTO>(
                projectPage.getCurrent(),
                projectPage.getSize(),
                projectPage.getTotal()
        );
        resultPage.setRecords(BeanUtil.copyToList(
                projectPage.getRecords(),
                ShortLinkStatsAccessRecordRespDTO.class
        ));
        return Results.success(resultPage);
    }

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        var projectRequest = objectMapper.convertValue(
                requestParam,
                com.nageoffer.shortlink.project.dto.req.ShortLinkGroupStatsAccessRecordReqDTO.class
        );
        var projectPage = projectShortLinkStatsService.groupShortLinkStatsAccessRecord(projectRequest);
        var resultPage = new Page<ShortLinkStatsAccessRecordRespDTO>(
                projectPage.getCurrent(),
                projectPage.getSize(),
                projectPage.getTotal()
        );
        resultPage.setRecords(BeanUtil.copyToList(
                projectPage.getRecords(),
                ShortLinkStatsAccessRecordRespDTO.class
        ));
        return Results.success(resultPage);
    }
}
