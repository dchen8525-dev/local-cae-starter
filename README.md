# Local CAE Job Service

本项目是一个本地 CAE 任务调度服务，基于 Spring Boot、SQLite 和 WebSocket。服务接收浏览器或脚本提交的任务请求，按 `tool` 类型调用对应适配器执行外部程序，并提供任务状态查询、取消和日志流。

当前内置适配器：

- `dummy_solver`
- `ansa`

服务默认监听 `127.0.0.1:8765`。

## 技术栈

- JDK 25
- Maven
- Spring Boot 3.5
- Spring Web
- Spring WebSocket
- Spring JDBC
- SQLite JDBC
- Jakarta Validation

## 项目结构

```text
.
├─ src/
│  └─ main/
│     ├─ java/com/local/caejobservice/
│     │  ├─ adapter/        # CAE 适配器与注册表
│     │  ├─ common/         # 配置、异常、工具类
│     │  └─ job/            # API、应用服务、持久化、日志流
│     └─ resources/
│        ├─ application.yml
│        └─ static/index.html
├─ data/                    # SQLite 数据库目录（运行时生成）
├─ workspaces/              # 每个任务的独立工作目录（运行时生成）
├─ pom.xml
└─ README.md
```

## 核心能力

- 通过 `POST /jobs` 提交任务
- 通过 `GET /jobs` 和 `GET /jobs/{jobId}` 查询任务
- 通过 `POST /jobs/{jobId}/cancel` 取消任务
- 通过 `ws://127.0.0.1:8765/ws/jobs/{jobId}` 实时读取日志
- 使用 SQLite 持久化任务状态
- 为每个任务创建独立工作目录和日志文件
- 服务重启后，将未完成的 `pending` 和 `running` 任务标记为 `failed`

## 环境要求

- 安装 JDK 25
- 安装 Maven 3.9+
- 如需执行 `ansa` 任务，需要本机已安装 ANSA，并配置可执行文件和脚本路径

## 启动方式

开发模式：

```bash
mvn spring-boot:run
```

打包运行：

```bash
mvn clean package
java -jar target/local-cae-job-service-0.1.0.jar
```

启动后可访问：

- 健康检查：`http://127.0.0.1:8765/`
- 静态演示页：`http://127.0.0.1:8765/index.html`
- 任务日志 WebSocket：`ws://127.0.0.1:8765/ws/jobs/<job_id>`

## 配置说明

主要配置位于 `src/main/resources/application.yml`。

常用项：

- `server.address`: 监听地址，默认 `127.0.0.1`
- `server.port`: 监听端口，默认 `8765`
- `app.database-path`: SQLite 文件路径，默认 `data/jobs.db`
- `app.workspace-root`: 工作目录根路径，默认 `workspaces`
- `app.allowed-origins`: HTTP 和 WebSocket 的允许来源
- `app.log-poll-interval-seconds`: WebSocket 轮询日志文件间隔
- `app.ansa-executable`: ANSA 可执行文件路径
- `app.ansa-script-file`: 默认 ANSA 脚本路径
- `app.ansa-execpy-prefix`: `-execpy` 前缀，默认 `load_script:`
- `app.ansa-batch-flags`: ANSA 无界面模式附加参数，默认 `-b`
- `app.ansa-candidate-paths`: 未显式配置 `app.ansa-executable` 时的候选路径

运行时产物：

- 数据库文件默认写入 `data/jobs.db`
- 每个任务的工作目录默认在 `workspaces/<job_id>/`
- 每个任务的日志文件默认在 `workspaces/<job_id>/run.log`

## API 概览

### 1. 健康检查

```http
GET /
```

示例响应：

```json
{
  "message": "Local CAE Job Service is running.",
  "frontend": "/frontend/index.html"
}
```

说明：响应里的 `frontend` 字段仍返回旧路径字符串，但当前静态页面文件实际位于 `/index.html`。

### 2. 提交任务

```http
POST /jobs
Content-Type: application/json
```

请求体：

```json
{
  "job_name": "demo-job",
  "tool": "dummy_solver",
  "params": {
    "duration": 3,
    "fail": false
  }
}
```

响应示例：

```json
{
  "job_id": "2a6a7f4f5dbf4e118f2c737f1dc4dbd0",
  "status": "pending",
  "message": "Job accepted and scheduled."
}
```

### 3. 查询单个任务

```http
GET /jobs/{jobId}
```

响应字段：

- `job_id`
- `job_name`
- `tool`
- `status`
- `pid`
- `return_code`
- `error_message`
- `workspace`
- `log_file`
- `created_at`
- `started_at`
- `finished_at`
- `params`

### 4. 查询任务列表

```http
GET /jobs
GET /jobs?status=running
```

支持按状态过滤，合法值来自：

- `pending`
- `running`
- `success`
- `failed`
- `cancelled`

### 5. 取消任务

```http
POST /jobs/{jobId}/cancel
```

### 6. 订阅任务日志

```text
ws://127.0.0.1:8765/ws/jobs/{jobId}
```

日志以文本片段方式持续推送；如果日志文件尚未生成，连接会收到错误消息后关闭。

## 适配器参数

### `dummy_solver`

用于本地联调和接口验证。

支持参数：

- `duration`: 运行秒数，整数，范围 `1` 到 `3600`，默认 `10`
- `fail`: 是否模拟失败，布尔值，默认 `false`

请求示例：

```bash
curl -X POST "http://127.0.0.1:8765/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "job_name": "dummy-demo",
    "tool": "dummy_solver",
    "params": {
      "duration": 3,
      "fail": false
    }
  }'
```

### `ansa`

用于调用本机 ANSA 批处理任务。

支持参数：

- `script_file`: 本次任务使用的脚本路径；未传时回退到 `app.ansa-script-file`
- `input_file`: 输入文件路径，可选
- `script_args`: 传给脚本的字符串数组，可选
- `extra_args`: 追加到 ANSA 命令尾部的字符串数组，可选
- `no_gui`: 是否追加批处理参数，默认 `true`

请求示例：

```bash
curl -X POST "http://127.0.0.1:8765/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "job_name": "ansa-batch-demo",
    "tool": "ansa",
    "params": {
      "input_file": "C:\\models\\demo.ansa",
      "script_args": ["--deck", "NASTRAN"],
      "extra_args": ["-mesa"],
      "no_gui": true
    }
  }'
```

执行前会校验：

- ANSA 可执行文件存在
- 脚本文件存在
- 如果提供了 `input_file`，则输入文件也必须存在

## 错误响应

请求校验失败、参数错误、未知工具或任务不存在时，会返回统一错误结构：

```json
{
  "timestamp": "2026-03-26T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "path": "/jobs",
  "detail": "Unknown tool 'demo'. Supported tools: [ansa, dummy_solver]"
}
```

## 本地开发建议

- 优先用 `dummy_solver` 验证提交流程、取消流程和日志流
- 接入真实 ANSA 前，先确认 `application.yml` 中路径配置可在当前机器访问
- `data/` 和 `workspaces/` 属于运行时目录，不应提交到版本库

## 测试

运行测试：

```bash
mvn test
```
