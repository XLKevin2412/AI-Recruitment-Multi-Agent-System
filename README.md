# AI Recruitment Multi-Agent System

## 中文版

### 项目简介

AI 招聘多智能体系统是在开源招聘 Demo 基础上的二次开发项目，目标是将单体演示升级为可本地运行的招聘辅助系统。项目采用 Java Backend、Python Agent Runtime 和 RAG Service 三层架构，接入 DeepSeek API、Milvus、Redis 和 Docker Compose，实现简历解析、人岗匹配分析、邮件草稿生成、面试建议和招聘问答流程闭环。

### 核心能力

- 招聘流程编排：Java Backend 统一管理岗位、候选人、申请、简历、分析报告、邮件草稿和面试建议。
- Agent Runtime：通过 FastAPI 封装简历分析、沟通生成、面试建议和招聘问答能力，支持 DeepSeek JSON 结构化输出和规则化 fallback。
- RAG 检索增强：支持文本切分、向量化、Milvus/内存双存储模式和相似度检索，为 Agent 提供岗位与简历证据上下文。
- 向量检索底座：基于 Milvus collection 存储招聘知识片段，使用 HNSW + COSINE 索引，并支持来源类型、申请 ID 和岗位 ID 过滤。
- 本地工程化部署：通过 Docker Compose 编排 MySQL、Redis、Milvus、RAG Service、Python Agent Runtime 和 Java Backend。

### 技术栈

- Backend: Java, Spring Boot, Spring Data JPA
- AI Runtime: Python, FastAPI, DeepSeek API, Pydantic, httpx
- RAG: Milvus, vector search, deterministic local embedding
- Infrastructure: MySQL, Redis, Docker Compose
- Testing: JUnit, pytest

### 服务结构

```text
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

### 测试

```powershell
cd python-agent-runtime
..\.venv\Scripts\python.exe -m pytest -q

cd ..\rag-service
..\.venv\Scripts\python.exe -m pytest -q

cd ..\java-backend
mvn test
```

### 当前边界

- 当前版本已完成 V1 本地闭环和真实 DeepSeek 调用验证。
- 面试能力当前是“面试建议生成”，未直接创建真实日历事件或会议链接。
- 项目未提供召回准确率、成本下降比例等评测指标；相关指标需要后续评测集和基准脚本支持。

## English Version

### Overview

AI Recruitment Multi-Agent System is a secondary development project based on an open-source recruitment demo. It upgrades the original demo into a locally runnable recruitment assistance system with a Java Backend, a Python Agent Runtime, and a RAG Service. The system integrates DeepSeek API, Milvus, Redis, and Docker Compose to support resume parsing, job-candidate matching analysis, email draft generation, interview suggestion generation, and recruitment Q&A.

### Key Features

- Recruitment workflow orchestration: Java Backend manages jobs, candidates, applications, resumes, analysis reports, email drafts, and interview suggestions.
- Agent Runtime: FastAPI exposes resume analysis, communication drafting, interview suggestion, and recruitment Q&A capabilities with DeepSeek JSON output and rule-based fallback.
- RAG enhancement: supports text chunking, vectorization, Milvus/memory storage modes, and similarity search to provide evidence context for agents.
- Vector retrieval foundation: uses a Milvus collection with HNSW + COSINE index and metadata filters such as source type, application ID, and job position ID.
- Local deployment: Docker Compose orchestrates MySQL, Redis, Milvus, RAG Service, Python Agent Runtime, and Java Backend.

### Tech Stack

- Backend: Java, Spring Boot, Spring Data JPA
- AI Runtime: Python, FastAPI, DeepSeek API, Pydantic, httpx
- RAG: Milvus, vector search, deterministic local embedding
- Infrastructure: MySQL, Redis, Docker Compose
- Testing: JUnit, pytest

### Architecture

```text
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

### Tests

```powershell
cd python-agent-runtime
..\.venv\Scripts\python.exe -m pytest -q

cd ..\rag-service
..\.venv\Scripts\python.exe -m pytest -q

cd ..\java-backend
mvn test
```

### Current Scope

- The current version provides a V1 local workflow and has been verified with a real DeepSeek smoke test.
- Interview capability currently generates interview suggestions; it does not directly create calendar events or meeting links.
- Metrics such as recall accuracy or cost reduction are not claimed because they require a dedicated evaluation dataset and benchmark scripts.
