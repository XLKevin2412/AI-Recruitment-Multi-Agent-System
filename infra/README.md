# 基础设施工作区

本工作区用于本地启动 AI 招聘多智能体系统依赖的基础组件。

## 服务

`docker-compose.yml` 包含：

- MySQL：业务数据库。
- Redis：缓存、任务状态、幂等锁。
- etcd：Milvus 元数据依赖。
- MinIO：Milvus 对象存储依赖。
- Milvus：RAG 向量库。

## 本地启动

```powershell
cd infra
copy .env.example .env
docker compose --env-file .env up -d
```

## 健康检查

```powershell
docker compose ps
```

预期结果：

- `mysql` healthy
- `redis` healthy
- `etcd` healthy
- `minio` healthy
- `milvus` healthy

## 停止服务

```powershell
docker compose down
```

## 本地数据

运行时数据默认写入：

```text
infra/data/mysql
infra/data/redis
infra/data/etcd
infra/data/minio
infra/data/milvus
```

这些目录不应提交到 GitHub。

