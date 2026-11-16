# ShortLink 短链接管理系统

ShortLink 将长网址转换为可分享的短链接，并提供分组管理、有效期设置、回收站和访问统计。从控制台创建一条短链接后，访问该地址即可跳转到原始网址，也可以在控制台查看这条链接的访问情况。

## 功能

- 短链接：单条或批量创建、分页查询、编辑描述与有效期；批量创建可导出结果。
- 管理：注册与登录、链接分组、移入回收站、恢复和彻底删除。
- 统计：按短链接或分组查询访问趋势和访问记录，查看 PV（访问次数）、UV（独立访客数）和 UIP（独立 IP 数）。

## 架构与目录

后端使用 JDK 17、Spring Boot 3.0.7 和 Maven；MySQL 保存业务数据，MyBatis-Plus 和 ShardingSphere-JDBC 负责数据访问与分片；Redis / Redisson 提供缓存和锁，RocketMQ 异步传递访问统计事件。控制台使用 Vue 3、Vite、Element Plus 和 ECharts。

| 目录 | 职责 |
| --- | --- |
| [aggregation](aggregation/) | 将管理模块与短链核心模块合并为一个可执行服务，默认端口 8003 |
| [admin](admin/) | 用户、分组和管理端 API |
| [project](project/) | 短链生成、跳转、回收站和统计处理 |
| [console-vue](console-vue/) | 管理控制台，开发环境将 /api 转发至 127.0.0.1:8003 |
| [gateway](gateway/) | 保留的网关模块，不参与默认聚合构建，本地快速启动无需网关或 Nacos |
| [resources/database](resources/database/) | 建表脚本和可选示例数据 |

跳转时，服务用请求的主机、端口和短码定位原始网址。正常路径优先查 Redis，未命中时通过布隆过滤器、空值缓存和锁减少无效查询与并发回源，再从数据库查找并回填缓存。统计事件由 RocketMQ 消费端异步处理，统计页面可能晚于跳转请求更新。

## 本地启动

以下命令以 Windows PowerShell 为例，从仓库根目录执行。准备 JDK 17、Maven、MySQL、Redis、RocketMQ（NameServer 和 Broker），以及用于运行控制台的 Node.js 和 npm。

### 1. 初始化数据库和中间件

连接 MySQL，将 [建表脚本](resources/database/link.sql) 导入 **link** 库。首次运行可以直接注册用户，[示例数据](resources/database/link-data.sql) 无需导入。

~~~powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS link CHARACTER SET utf8mb4;"
mysql -u root -p link -e "SOURCE resources/database/link.sql"
~~~

启动 Redis 和 RocketMQ，确保应用能连接到 NameServer 及其返回的 Broker 地址。如果 Broker 没有开启自动建主题，预先创建主题 **short-link-stats**。

| 配置 | 本地启动值 | 修改方式 |
| --- | --- | --- |
| MySQL | 127.0.0.1:3306，库名 link | 设置 MYSQL_HOST、MYSQL_PORT、MYSQL_USERNAME、MYSQL_PASSWORD |
| Redis | 127.0.0.1:6379 | 在聚合服务配置中修改 spring.data.redis 下的连接参数 |
| RocketMQ NameServer | 127.0.0.1:9876 | 设置 ROCKETMQ_NAME_SERVER |
| 短链对外地址 | 建议使用 localhost:8003 | 设置 SHORT_LINK_DOMAIN，只写主机和端口 |

### 2. 启动聚合后端

将以下 MySQL 用户名和密码替换为能够访问 link 库的本机账号。[聚合服务配置](aggregation/src/main/resources/application.yaml) 中的短链域名默认指向其他主机，本地运行时应显式覆盖。

~~~powershell
$env:MYSQL_USERNAME = 'root'
$env:MYSQL_PASSWORD = '<你的 MySQL 密码>'
$env:SHORT_LINK_DOMAIN = 'localhost:8003'
mvn -pl aggregation -am package -DskipTests
java -jar aggregation/target/shortlink-aggregation.jar
~~~

### 3. 启动管理控制台

另开一个 PowerShell 窗口，在仓库根目录执行：

~~~powershell
Set-Location console-vue
npm ci
npm run dev
~~~

打开 Vite 输出的 Local 地址，通常是 http://localhost:5173。部署前端时可用 **npm run build** 生成 dist，并在部署环境配置 /api 到聚合后端的转发；Vite 的开发代理只在开发服务器中生效。

## 验证完整链路

1. 在控制台注册并登录，选择一个分组，创建指向 https://example.com/ 的永久短链接。
2. 复制控制台返回的短链接，在浏览器打开，确认跳转到原址。也可用 **curl.exe -i "http://localhost:8003/替换为实际短码"** 检查 GET 响应是否为 302，以及 Location 是否指向原址。
3. 回到控制台查看该链接的访问统计，选择包含今天的时间范围，等待消费完成后刷新。
4. 将链接移入回收站，确认它不能再跳转到原址；恢复后再次验证跳转。

| 现象 | 优先检查 |
| --- | --- |
| 控制台 API 请求失败 | 聚合服务是否监听 8003，以及 [Vite 代理配置](console-vue/vite.config.js) 是否指向该端口 |
| MySQL 提示表不存在 | 建表脚本是否导入 link 库，以及连接配置是否指向同一个实例 |
| 刚创建的链接跳转到未找到页面 | 访问地址的主机和端口是否与创建时的 SHORT_LINK_DOMAIN 一致；localhost 和 127.0.0.1 不可混用 |
| 跳转正常，统计不更新 | 检查主题、Broker 连通性、生产和消费日志，以及统计查询的时间范围 |

## API 入口

管理 API 的共同前缀为 **/api/short-link/admin/v1**，下表省略该前缀。登录后的管理请求可参考 [控制台请求封装](console-vue/src/api/axios.js)。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | /user | 注册 |
| POST | /user/login | 登录 |
| GET / POST | /group | 查询 / 创建分组 |
| POST | /create、/create/batch | 单条 / 批量创建 |
| GET | /page | 分页查询短链接 |
| POST | /update | 编辑短链接 |
| POST | /recycle-bin/save、/recycle-bin/recover、/recycle-bin/remove | 移入、恢复、彻底删除 |
| GET | /stats、/stats/group | 查询单条链接 / 分组统计 |

公开跳转入口为 **GET /{shortUri}**，直接使用生成的完整短链访问，无需登录。

