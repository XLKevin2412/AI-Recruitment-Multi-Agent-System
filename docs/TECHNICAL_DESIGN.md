# AI 招聘多智能体系统技术设计

## 1. 项目目标

本项目参考 `awesome-llm-apps` 中的 AI Recruitment Agent Team 示例，但将其从单文件 Python/Streamlit 演示升级为面向工程落地的多服务骨架。

系统目标是构建一个 Java + Python + RAG + MySQL + Redis + Milvus + DeepSeek 的招聘多智能体系统，围绕候选人简历分析、邮件沟通、面试安排三个核心招聘环节提供可扩展的自动化能力。

本阶段只初始化项目骨架、工作空间和技术文档，不实现业务代码、不创建依赖文件、不提供可运行服务入口。

## 2. 核心业务流程

1. 候选人提交简历、目标岗位和联系方式。
2. Java 后端保存候选人、岗位、申请记录和流程状态。
3. RAG Service 对简历、岗位要求、招聘知识、面试题库进行切分、向量化和检索。
4. Python Agent Runtime 调用 DeepSeek API，并编排三个智能体完成招聘任务。
5. 简历分析 Agent 结合简历文本、岗位要求和 RAG 检索结果生成评分、匹配理由和建议。
6. 邮件沟通 Agent 根据分析结果生成候选人沟通邮件，实际发送由邮件适配器完成。
7. 面试安排 Agent 根据候选人状态、面试官可用时间和会议策略生成面试安排，实际日历和会议创建由适配器完成。
8. Java 后端统一记录邮件、面试、Agent 调用结果和业务状态。

## 3. 工作空间职责

### `workspaces/java-backend`

Java 后端工作区，后续负责：

- 对外 REST API。
- 用户、权限、岗位、候选人、申请流程等业务模型。
- MySQL 业务数据读写。
- Redis 缓存、任务状态、幂等锁和短期会话。
- 调用 Python Agent Runtime 和 RAG Service。
- 统一鉴权、审计、异常处理和流程状态机。

### `workspaces/python-agent-runtime`

Python 智能体运行时工作区，后续负责：

- 三个智能体的提示词、工具调用和协作编排。
- DeepSeek API 调用封装。
- Agent 输入输出结构校验。
- 邮件、日历、会议、RAG 等工具适配器调用。
- Agent 调用日志、失败重试和结果归档。

保留三个核心智能体：

- 简历分析 Agent：解析简历，匹配岗位要求，结合 RAG 检索结果输出候选人评分、优势、风险和建议。
- 邮件沟通 Agent：生成录用、拒绝、补充材料、面试邀请、改期确认等邮件内容。
- 面试安排 Agent：生成候选面试时间，协调日历，创建会议，输出面试安排结果。

### `workspaces/rag-service`

RAG 服务工作区，后续负责：

- 简历、岗位说明、招聘知识、面试题库的文本切分。
- 向量化任务编排。
- Milvus 向量写入和检索。
- 检索结果重排、过滤和上下文拼装。
- 为简历分析 Agent 提供岗位匹配证据和面试建议依据。

### `workspaces/mysql`

MySQL 工作区，后续放置数据库设计、迁移脚本和初始化数据。

建议保存的数据包括：

- 候选人信息。
- 岗位信息。
- 简历元数据。
- 申请流程状态。
- 简历分析结果。
- 邮件草稿和发送记录。
- 面试安排记录。
- Agent 调用审计记录。

### `workspaces/redis`

Redis 工作区，后续放置缓存和队列相关设计。

Redis 主要用于：

- 短期会话状态。
- Agent 任务状态。
- 幂等锁。
- 接口限流。
- RAG 热点检索缓存。
- DeepSeek 调用结果短期缓存。

### `workspaces/milvus`

Milvus 工作区，后续放置向量库集合设计、索引策略和本地初始化说明。

建议保存的向量数据包括：

- 简历文本片段。
- 岗位要求片段。
- 面试题库片段。
- 招聘规范和沟通模板片段。
- 历史评估反馈片段。

### `workspaces/infra`

基础设施工作区，后续负责系统运行和部署相关配置。

可放置：

- Docker Compose。
- Kubernetes 配置。
- Nginx 或 API Gateway 配置。
- 本地开发环境脚本。
- CI/CD 配置。
- MySQL、Redis、Milvus 的本地启动配置。
- 环境变量模板。

### `workspaces/observability`

可观测性工作区，后续负责日志、指标、链路追踪和告警。

可放置：

- 日志规范。
- Prometheus 指标定义。
- Grafana Dashboard。
- 链路追踪配置。
- Agent 调用审计方案。
- DeepSeek 调用耗时、失败率、token 用量统计。
- RAG 检索命中率和召回质量指标。
- 告警规则。

## 4. 技术架构

```text
Client / Admin UI
        |
        v
Java Backend
        |
        +--> MySQL
        +--> Redis
        +--> Python Agent Runtime
                    |
                    +--> DeepSeek API
                    +--> RAG Service
                    |       |
                    |       +--> Milvus
                    |
                    +--> Email Provider Adapter
                    +--> Calendar Provider Adapter
                    +--> Meeting Provider Adapter
```

### Java Backend

Java 后端是系统的业务入口，负责稳定的流程控制和数据一致性。它不直接承载复杂提示词和 Agent 编排，避免让业务 API 与大模型调用细节耦合。

### Python Agent Runtime

Python 更适合快速接入 LLM SDK、RAG 工具链和 Agent 编排框架。三个智能体统一运行在该工作区，Java 后端通过内部 API 或任务队列调用它。

### RAG Service

RAG Service 独立出来，避免简历分析 Agent 直接操作向量库。这样后续可以替换 embedding 模型、重排策略或向量库索引，而不影响业务 API 和 Agent 主流程。

## 5. DeepSeek API 接入方案

DeepSeek 调用由 Python Agent Runtime 统一封装，后续通过环境变量配置：

```text
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=
```

设计原则：

- 不在业务代码中硬编码 API Key。
- Java 后端不直接调用 DeepSeek，统一通过 Python Agent Runtime 间接调用。
- 所有 DeepSeek 请求记录调用耗时、失败原因和 token 用量。
- Agent 输出必须做结构化校验，不能直接信任模型自然语言结果。
- 对简历评分、拒绝建议等敏感决策保留人工复核入口。

## 6. 适配器设计

邮件、日历、会议不直接绑定 Gmail、Zoom 或企业系统，而是先定义适配器边界。

### Email Provider Adapter

负责发送或保存邮件草稿。后续可实现：

- Gmail。
- Outlook。
- 飞书。
- 企业微信。
- SMTP。

### Calendar Provider Adapter

负责查询可用时间和创建日历事件。后续可实现：

- Google Calendar。
- Outlook Calendar。
- 飞书日历。
- 企业微信日历。

### Meeting Provider Adapter

负责创建会议链接。后续可实现：

- Zoom。
- 腾讯会议。
- 飞书会议。
- Microsoft Teams。

这样可以让三个智能体专注招聘逻辑，不被具体供应商 API 锁死。

## 7. 数据职责划分

### MySQL

MySQL 保存长期、结构化、可审计的业务数据：

- 候选人。
- 岗位。
- 简历文件元数据。
- 申请记录。
- 分析报告。
- 邮件记录。
- 面试记录。
- Agent 调用记录。

### Redis

Redis 保存短期、高频、可过期的数据：

- 登录会话。
- 任务进度。
- 幂等锁。
- 接口限流计数。
- 热点岗位要求缓存。
- Agent 中间状态。

### Milvus

Milvus 保存向量化后的非结构化知识：

- 简历片段向量。
- 岗位要求向量。
- 面试题库向量。
- 招聘知识向量。
- 历史反馈向量。

## 8. 后续演进路线

1. 补充 Java 后端基础工程、API 合约和 MySQL schema。
2. 补充 Python Agent Runtime，先实现 DeepSeek 调用封装和三个 Agent 的结构化输出。
3. 补充 RAG Service，完成 Milvus 集合设计、文本切分和检索接口。
4. 补充 Redis 任务状态和幂等控制。
5. 增加邮件、日历、会议的第一批供应商适配器。
6. 增加 observability 指标、调用审计和错误告警。
7. 增加端到端验收流程：上传简历、分析、生成邮件、生成面试安排。
