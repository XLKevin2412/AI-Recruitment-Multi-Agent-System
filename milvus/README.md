# Milvus 工作区

本工作区用于记录 Milvus collection、索引和健康检查方式。

## 部署模式

V1 本地使用 Milvus Standalone。

依赖：

- etcd
- MinIO

这些依赖由 `infra/docker-compose.yml` 编排。

## Collection

V1 建议使用一个统一 collection：

```text
recruitment_knowledge_chunks
```

## 字段规划

| 字段 | 说明 |
| --- | --- |
| id | chunk ID |
| source_type | RESUME / JOB_REQUIREMENT / KNOWLEDGE / TEMPLATE |
| source_id | 来源业务 ID |
| application_id | 申请 ID，可为空 |
| job_position_id | 岗位 ID，可为空 |
| chunk_index | 分片序号 |
| content | 文本片段 |
| embedding | 向量字段 |
| metadata_json | 元数据 |
| created_at | 创建时间 |

## 索引建议

```text
index_type: HNSW
metric_type: COSINE
```

## 健康检查

基础检查：

```powershell
docker compose -f infra/docker-compose.yml ps milvus
```

后续 RAG Service 应提供 smoke test：

```text
连接 Milvus
列出 collection
写入测试向量
执行一次 topK 检索
删除测试数据
```

