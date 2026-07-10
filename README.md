# AI Recruitment Multi-Agent System

## 中文版

### 项目简介

AI 招聘多智能体系统是在开源招聘 Demo 基础上的二次开发项目，目标是将单体演示升级为可本地运行的招聘辅助系统。项目采用 Vue Admin、Java Backend、Python Agent Runtime 和 RAG Service 分层架构，支持接入 DeepSeek API，并使用 Milvus、Redis 和 Docker Compose，实现简历解析、人岗匹配分析、邮件草稿生成、面试建议和招聘问答流程闭环；未配置 DeepSeek 凭据时，Agent Runtime 会使用规则化 fallback。

### 核心能力

- 招聘流程编排：Java Backend 统一管理岗位、候选人、申请、简历、分析报告、邮件草稿和面试建议。
- Vue 管理端：提供可视化工作台，支持创建岗位、候选人、申请记录，上传简历，触发分析并查看 Agent 输出。
- Agent Runtime：通过 FastAPI 封装简历分析、沟通生成、面试建议和招聘问答能力，支持 DeepSeek JSON 结构化输出和规则化 fallback。
- RAG 检索增强：支持文本切分、向量化、Milvus/内存双存储模式（`auto` 模式可在 Milvus 不可用时回退）和相似度检索，为 Agent 提供岗位与简历证据上下文。
- 向量检索底座：Milvus 模式下基于 collection 存储招聘知识片段，使用 HNSW + COSINE 索引，并支持来源类型、申请 ID 和岗位 ID 过滤；内存模式用于本地回退。
- 本地工程化部署：通过 Docker Compose 编排 MySQL、Redis、Milvus、RAG Service、Python Agent Runtime、Java Backend 和 Vue Admin。

### 技术栈

- Backend: Java, Spring Boot, Spring Data JPA
- Frontend: Vue 3, Vite
- AI Runtime: Python, FastAPI, DeepSeek API, Pydantic, httpx
- RAG: Milvus, vector search, deterministic local embedding
- Infrastructure: MySQL, Redis, Docker Compose
- Testing: JUnit, pytest

### 服务结构

```text
Vue Admin
  -> Java Backend: browser-facing API

Java Backend
  -> MySQL: business data
  -> Redis: cache
  -> Python Agent Runtime: agent capabilities
  -> RAG Service: evidence retrieval

Python Agent Runtime
  -> DeepSeek API: structured LLM output
  -> rule-based fallback

RAG Service
  -> Milvus: vector storage and retrieval
  -> memory store: local fallback
```

### 本地运行

1. 复制环境变量文件：

```powershell
cd infra
copy .env.example .env
```

2. 在 `infra/.env` 中配置 DeepSeek API Key：

```env
DEEPSEEK_API_KEY=your_api_key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_TIMEOUT_SECONDS=30
```

3. 启动服务：

```powershell
docker compose --env-file .env up -d
```

4. 打开管理端页面：

```text
http://localhost:3000
```

页面操作顺序为：创建岗位 -> 创建候选人 -> 创建申请 -> 上传简历 -> 触发分析 -> 查看邮件草稿、面试建议和招聘问答。

### 测试

```powershell
cd frontend/admin
npm test
npm run build

cd ..\..\python-agent-runtime
..\.venv\Scripts\python.exe -m pytest -q

cd ..\rag-service
..\.venv\Scripts\python.exe -m pytest -q

cd ..\java-backend
mvn test
```

### 当前边界

- 当前版本已完成 V1 本地闭环；DeepSeek 路径支持真实调用，但是否使用真实模型取决于 `DEEPSEEK_API_KEY` 和运行环境，未配置凭据或调用失败时会自动 fallback。仓库中的 Agent 测试使用模拟客户端，真实 Provider smoke test 需要按环境单独执行。
- 面试能力当前是“面试建议生成”，未直接创建真实日历事件或会议链接。
- 当前管理端是本地开发与演示工作区，未实现登录、角色权限和租户隔离，不应直接作为公网服务部署。
- Java Backend 对简历上传执行 10 MiB 大小限制和 PDF 扩展名、文件头校验；这不等同于恶意文档查杀，生产环境仍需增加病毒扫描和隔离解析。
- 项目未提供召回准确率、成本下降比例等评测指标；相关指标需要后续评测集和基准脚本支持。

## English Version

### Overview

AI Recruitment Multi-Agent System is a secondary development project based on an open-source recruitment demo. It upgrades the original demo into a locally runnable recruitment assistance system with Vue Admin, a Java Backend, a Python Agent Runtime, and a RAG Service. The system supports the DeepSeek API and uses Milvus, Redis, and Docker Compose to provide resume parsing, job-candidate matching analysis, email draft generation, interview suggestion generation, and recruitment Q&A. When DeepSeek credentials are not configured, the Agent Runtime uses a rule-based fallback.

### Key Features

- Recruitment workflow orchestration: Java Backend manages jobs, candidates, applications, resumes, analysis reports, email drafts, and interview suggestions.
- Vue Admin: provides a browser workspace for creating jobs, candidates, applications, uploading resumes, triggering analysis, and reviewing agent outputs.
- Agent Runtime: FastAPI exposes resume analysis, communication drafting, interview suggestion, and recruitment Q&A capabilities with DeepSeek JSON output and rule-based fallback.
- RAG enhancement: supports text chunking, vectorization, Milvus/memory storage modes (`auto` can fall back when Milvus is unavailable), and similarity search to provide evidence context for agents.
- Vector retrieval foundation: in Milvus mode, uses a collection with an HNSW + COSINE index and metadata filters such as source type, application ID, and job position ID; memory mode is available as a local fallback.
- Local deployment: Docker Compose orchestrates MySQL, Redis, Milvus, RAG Service, Python Agent Runtime, Java Backend, and Vue Admin.

### Tech Stack

- Backend: Java, Spring Boot, Spring Data JPA
- Frontend: Vue 3, Vite
- AI Runtime: Python, FastAPI, DeepSeek API, Pydantic, httpx
- RAG: Milvus, vector search, deterministic local embedding
- Infrastructure: MySQL, Redis, Docker Compose
- Testing: JUnit, pytest

### Architecture

```text
Vue Admin
  -> Java Backend: browser-facing API

Java Backend
  -> MySQL: business data
  -> Redis: cache
  -> Python Agent Runtime: agent capabilities
  -> RAG Service: evidence retrieval

Python Agent Runtime
  -> DeepSeek API: structured LLM output
  -> rule-based fallback

RAG Service
  -> Milvus: vector storage and retrieval
  -> memory store: local fallback
```

### Local Setup

1. Copy the environment file:

```powershell
cd infra
copy .env.example .env
```

2. Configure the DeepSeek API key in `infra/.env`:

```env
DEEPSEEK_API_KEY=your_api_key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_TIMEOUT_SECONDS=30
```

3. Start services:

```powershell
docker compose --env-file .env up -d
```

4. Open the admin page:

```text
http://localhost:3000
```

Workflow: create a job -> create a candidate -> create an application -> upload a resume -> trigger analysis -> review email draft, interview suggestion, and recruitment Q&A.

### Tests

```powershell
cd frontend/admin
npm test
npm run build

cd ..\..\python-agent-runtime
..\.venv\Scripts\python.exe -m pytest -q

cd ..\rag-service
..\.venv\Scripts\python.exe -m pytest -q

cd ..\java-backend
mvn test
```

### Current Scope

- The current version provides a V1 local workflow. The DeepSeek path supports real-provider calls, but actual model use depends on `DEEPSEEK_API_KEY` and the runtime environment; missing credentials or failed calls trigger the rule-based fallback. Repository Agent tests use a mocked client, while a real-provider smoke test must be run separately in the target environment.
- Interview capability currently generates interview suggestions; it does not directly create calendar events or meeting links.
- The admin workspace is intended for local development and demos; authentication, role-based authorization, and tenant isolation are not implemented, so it must not be exposed directly to the public internet.
- Java Backend limits resume uploads to 10 MiB and validates the PDF extension and file signature. This is not malware scanning; production deployments still need antivirus scanning and isolated document parsing.
- Metrics such as recall accuracy or cost reduction are not claimed because they require a dedicated evaluation dataset and benchmark scripts.
