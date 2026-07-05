# RAG Service

本模块是 AI 招聘系统的 RAG 检索服务，负责文档切分、向量化、向量存储和相似度检索，为简历分析、岗位匹配和问答提供证据上下文。

## 服务职责

- 接收简历、岗位要求等文本内容。
- 将长文本切分为可检索片段。
- 为文本片段生成向量。
- 将向量写入内存存储或 Milvus。
- 根据查询语句返回相似片段和证据 ID。

当前 V1 使用确定性的本地 embedding，便于无外部模型凭据时完成本地演示。在 Docker Compose 中，`RAG_STORAGE_BACKEND=milvus` 会将向量写入 Milvus 并从 Milvus 检索。本地开发默认使用 `auto` 模式，如果 Milvus 不可用，会回退到内存存储。

## 接口列表

- `GET /health`
- `POST /internal/rag/documents`
- `POST /internal/rag/search`

## 配置项

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `RAG_STORAGE_BACKEND` | `auto` | 存储后端，可选 `auto`、`milvus` 或 `memory`。 |
| `MILVUS_HOST` | `localhost` | Milvus 主机地址。 |
| `MILVUS_GRPC_PORT` | `19530` | Milvus gRPC 端口。 |
| `MILVUS_COLLECTION` | `recruitment_knowledge_chunks` | Milvus collection 名称。 |
| `EMBEDDING_DIMENSION` | `128` | 当前确定性 embedding 使用的向量维度。 |
| `RAG_CHUNK_SIZE` | `700` | 单个文本分片的最大字符数。 |
| `RAG_CHUNK_OVERLAP` | `120` | 相邻文本分片的重叠字符数。 |

## 本地运行

```powershell
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8200
```

默认访问地址：

```text
http://localhost:8200
```

## 后续演进

- 将确定性 embedding 替换为真实 embedding 模型。
- 增加 RAG 检索缓存和重排策略。
- 补充 Milvus collection 初始化和 smoke test 自动化。
