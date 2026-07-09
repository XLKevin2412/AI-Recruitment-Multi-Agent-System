# AI 招聘系统 Java 后端

本模块是 V1 招聘流程的 Spring Boot 业务后端，负责候选人、岗位、申请流程、简历记录、AI 评估结果、邮件草稿、面试建议和问答入口等业务能力。

## 当前状态

本模块已接入 Python Agent Runtime 和 RAG Service，负责统一编排简历解析、RAG 检索、分析报告、邮件草稿、面试建议和招聘问答流程。

## 本地运行

先启动依赖服务：

```powershell
docker compose -f ..\infra\docker-compose.yml --env-file ..\infra\.env up -d
```

再设置 MySQL 和 Redis 环境变量。如果本地 `infra\.env` 使用当前开发默认值，可以使用：

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="13306"
$env:MYSQL_DATABASE="ai_recruitment"
$env:MYSQL_USERNAME="ai_recruitment"
$env:MYSQL_PASSWORD="change_me"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:8080/api
```

## 主要接口

- `GET /api/health`
- `GET /api/actuator/health`
- `GET /api/job-templates`
- `GET /api/jobs?page=1&pageSize=20`
- `POST /api/jobs`
- `GET /api/jobs/{id}`
- `PUT /api/jobs/{id}`
- `DELETE /api/jobs/{id}`
- `GET /api/candidates?page=1&pageSize=20`
- `POST /api/candidates`
- `GET /api/candidates/{id}`
- `PUT /api/candidates/{id}`
- `DELETE /api/candidates/{id}`
- `GET /api/applications?page=1&pageSize=20`
- `POST /api/applications`
- `GET /api/applications/{id}`
- `PUT /api/applications/{id}`
- `DELETE /api/applications/{id}`
- `POST /api/applications/{id}/resume`
- `POST /api/applications/{id}/analysis`
- `GET /api/applications/{id}/analysis`
- `POST /api/applications/{id}/emails/draft`
- `GET /api/applications/{id}/emails`
- `POST /api/applications/{id}/interviews/propose`
- `GET /api/applications/{id}/interviews`
- `POST /api/applications/{id}/qa`

## 申请流程规则

- `POST /api/applications` 创建申请时，只允许初始状态为 `DRAFT` 或 `SUBMITTED`。
- `PUT /api/applications/{id}` 只更新申请状态、绑定简历和当前阶段。
- 申请创建后，候选人和岗位绑定关系不可变。
- 非法状态流转返回 `409 Conflict`。

## V1 集成说明

- 后端通过 `AGENT_RUNTIME_BASE_URL` 调用 Python Agent Runtime。
- 后端通过 `RAG_SERVICE_BASE_URL` 调用 RAG Service。
- 简历文件存储在 `RESUME_STORAGE_DIR` 指定目录。
- Redis 当前用于岗位查询缓存和最新分析结果缓存；缓存失败不会中断业务请求。
- 当前持久化层使用 Spring Data JPA。原目标中提到 MyBatis，如需严格对齐，可在保持 REST API 不变的前提下迁移 Repository 层。
