# Milvus 工作区

本工作区用于记录 Milvus collection、索引策略、字段规划和健康检查方式。

## 当前状态

RAG Service 已支持通过 `RAG_STORAGE_BACKEND=milvus` 写入和检索 Milvus；本地开发也可以使用 `auto` 或 `memory` 模式。

## 部署模式

V1 本地使用 Milvus Standalone。

依赖组件：

- etcd
- MinIO

这些依赖由 `infra/docker-compose.yml` 统一编排。

## Collection

V1 使用统一 collection：

```text
recruitment_knowledge_chunks
```

该 collection 用于保存简历、岗位要求、招聘知识和面试相关文本的向量片段。

## 字段规划

| 字段 | 说明 |
| --- | --- |
| `evidence_id` | RAG 证据 ID，也是向量片段主键。 |
| `document_id` | 文档 ID。 |
| `source_type` | 来源类型，例如 `RESUME`、`JOB_REQUIREMENT`、`KNOWLEDGE`、`TEMPLATE`。 |
| `source_id` | 来源业务 ID。 |
| `application_id` | 申请 ID，可为空。 |
| `job_position_id` | 岗位 ID，可为空。 |
| `chunk_id` | 文本分片 ID。 |
| `content` | 文本片段内容。 |
| `metadata_json` | 元数据 JSON 字符串。 |
| `created_at` | 创建时间。 |
| `embedding` | 向量字段。 |

## 索引建议

```text
index_type: HNSW
metric_type: COSINE
```

当前 RAG Service 会根据配置读取向量维度，并在 collection 不存在时创建索引。

## 健康检查

查看容器状态：

```powershell
docker compose -f infra/docker-compose.yml ps milvus
```

RAG Service 的后续 smoke test 应覆盖：

```text
连接 Milvus
列出 collection
写入测试向量
执行一次 topK 检索
删除测试数据
```
