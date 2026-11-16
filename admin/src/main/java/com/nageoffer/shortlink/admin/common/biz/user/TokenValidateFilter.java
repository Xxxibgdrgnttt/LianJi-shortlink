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

package com.nageoffer.shortlink.admin.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;

/**
 * 应用内 Token 校验过滤器
 */
@RequiredArgsConstructor
public class TokenValidateFilter implements Filter {

    private static final String SHORT_LINK_API_PREFIX = "/api/short-link/";
    private static final String USER_API_PATH = "/api/short-link/admin/v1/user";
    private static final Set<String> WHITE_PATH_LIST = Set.of(
            "/api/short-link/admin/v1/user/login",
            "/api/short-link/admin/v1/user/has-username"
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith(SHORT_LINK_API_PREFIX) || isWhitePath(requestPath, request.getMethod())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String username = request.getHeader("username");
        String token = request.getHeader("token");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(token)) {
            writeUnauthorized(response);
            return;
        }

        Object userInfo = stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token);
        if (userInfo == null) {
            writeUnauthorized(response);
            return;
        }
        JSONObject userInfoJson = JSON.parseObject(userInfo.toString());
        String realName = URLEncoder.encode(userInfoJson.getString("realName"), StandardCharsets.UTF_8);
        filterChain.doFilter(new AuthenticatedRequest(request, username, userInfoJson.getString("id"), realName), response);
    }

    private boolean isWhitePath(String requestPath, String requestMethod) {
        return WHITE_PATH_LIST.contains(requestPath)
                || ("POST".equals(requestMethod) && USER_API_PATH.equals(requestPath));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(
                Map.of("status", HttpStatus.UNAUTHORIZED.value(), "message", "Token validation error")
        ));
    }

    private static final class AuthenticatedRequest extends HttpServletRequestWrapper {

        private final String username;
        private final String userId;
        private final String realName;

        private AuthenticatedRequest(HttpServletRequest request, String username, String userId, String realName) {
            super(request);
            this.username = username;
            this.userId = userId;
            this.realName = realName;
        }

        @Override
        public String getHeader(String name) {
            return switch (name) {
                case "username" -> username;
                case "userId" -> userId;
                case "realName" -> realName;
                default -> super.getHeader(name);
            };
        }
    }
}
