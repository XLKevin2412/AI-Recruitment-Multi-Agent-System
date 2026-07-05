# Python Agent Runtime

本模块是 AI 招聘系统的 Python 智能体运行时服务，使用 FastAPI 对外提供结构化的内部接口，供 Java 后端调用。

## 服务职责

- 解析 PDF 简历并返回结构化解析状态。
- 根据岗位要求、候选人简历和 RAG 证据生成简历分析结果。
- 生成候选人沟通邮件草稿。
- 生成面试安排建议。
- 基于 RAG 上下文回答招聘流程中的问题。

当前版本优先保证本地闭环可演示。如果没有配置 `DEEPSEEK_API_KEY` 和 `DEEPSEEK_MODEL`，服务会使用确定性的规则化输出，避免本地开发必须依赖外部大模型凭据。

## 接口列表

- `GET /health`
- `POST /internal/agents/resume-parse`
- `POST /internal/agents/resume-analysis`
- `POST /internal/agents/email-draft`
- `POST /internal/agents/interview-plan`
- `POST /internal/agents/qa`

## 本地运行

```powershell
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8100
```

默认访问地址：

```text
http://localhost:8100
```

## 后续演进

- 接入 DeepSeek 结构化调用。
- 将规则化 Agent 输出替换为提示词和模型生成结果。
- 补充 Agent 调用失败重试、超时控制和更完整的审计信息。
