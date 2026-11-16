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

package com.nageoffer.shortlink.aggregation.config;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@Configuration
public class ShardingSphereDataSourceConfiguration {

    @Bean
    public DataSource dataSource(Environment environment) throws IOException, SQLException {
        String databaseEnv = environment.getProperty("database.env", "dev");
        String config = new ClassPathResource("shardingsphere-config-" + databaseEnv + ".yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        config = config
                .replace("${MYSQL_HOST:127.0.0.1}", environment.getProperty("MYSQL_HOST", "127.0.0.1"))
                .replace("${MYSQL_PORT:3306}", environment.getProperty("MYSQL_PORT", "3306"))
                .replace("${MYSQL_USERNAME:root}", environment.getProperty("MYSQL_USERNAME", "root"))
                .replace("${MYSQL_PASSWORD:root}", environment.getProperty("MYSQL_PASSWORD", "root"));
        return YamlShardingSphereDataSourceFactory.createDataSource(config.getBytes(StandardCharsets.UTF_8));
    }
}
