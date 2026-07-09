# 基础设施工作区

本工作区用于本地启动 AI 招聘多智能体系统依赖的基础组件，并统一管理开发环境端口、容器编排和运行时数据目录。

## 服务列表

`docker-compose.yml` 包含以下服务：

- MySQL：业务数据库。
- Redis：缓存、任务状态和幂等锁。
- etcd：Milvus 元数据依赖。
- MinIO：Milvus 对象存储依赖。
- Milvus：RAG 向量数据库。
- RAG Service：文档切分和向量检索服务。
- Python Agent Runtime：智能体运行时服务。
- Java Backend：招聘业务后端。
- Frontend Admin：Vue 招聘管理端，用于创建岗位、候选人、申请记录，上传简历并触发 Agent 分析。

## 关键运行时配置

- `RAG_STORAGE_BACKEND=milvus`：Compose 默认让 RAG Service 使用 Milvus；本地单独开发可改为 `auto` 或 `memory`。
- `MILVUS_COLLECTION=recruitment_knowledge_chunks`：RAG 片段统一写入该 collection，通过来源类型和业务 ID 过滤。
- `DEEPSEEK_MODEL=deepseek-v4-flash`：Python Agent Runtime 默认模型；未配置 `DEEPSEEK_API_KEY` 时自动使用规则化 fallback。
- `DEEPSEEK_TIMEOUT_SECONDS=30`：DeepSeek 单次请求超时时间。

## 本地启动

```powershell
cd infra
copy .env.example .env
docker compose --env-file .env up -d
```

如果只需要启动基础设施，也可以在 Compose 中指定单个服务或服务组。

启动完成后访问：

```text
http://localhost:3000
```

页面通过 `/api` 代理访问 Java Backend；Python Agent Runtime 和 RAG Service 仍由 Java Backend 在后端内部调用。

## 健康检查

```powershell
docker compose ps
```

基础设施服务的预期状态：

- `mysql` healthy
- `redis` healthy
- `etcd` healthy
- `minio` healthy
- `milvus` healthy

业务服务预期状态：

- `rag-service` running
- `python-agent-runtime` running
- `java-backend` running
- `frontend-admin` running

## 停止服务

```powershell
docker compose down
```

如需同时删除本地运行数据，需要手动清理 `infra/data`。执行前应确认没有需要保留的测试数据。

## 本地数据目录

运行时数据默认写入：

```text
infra/data/mysql
infra/data/redis
infra/data/etcd
infra/data/minio
infra/data/milvus
infra/data/resumes
```

这些目录保存本地数据库、缓存、对象存储、向量库和简历文件，不应提交到 GitHub。
