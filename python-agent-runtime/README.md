# Python Agent Runtime

本模块是 AI 招聘系统的 Python 智能体运行时服务，使用 FastAPI 对外提供结构化的内部接口，供 Java 后端调用。

## 服务职责

- 解析 PDF 简历并返回结构化解析状态。
- 根据岗位要求、候选人简历和 RAG 证据生成简历分析结果。
- 生成候选人沟通邮件草稿。
- 生成面试安排建议。
- 基于 RAG 上下文回答招聘流程中的问题。

当前版本优先保证本地闭环可演示。服务会优先使用 DeepSeek 的 OpenAI 兼容 `chat/completions` 接口和 JSON Output 生成结构化结果；如果没有配置 `DEEPSEEK_API_KEY`，或模型调用失败、返回空内容、返回非 JSON、字段校验失败，接口会自动回退到确定性的规则化输出，避免本地开发必须依赖外部大模型凭据。

## DeepSeek 配置

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key；为空时使用规则化 fallback。 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容 API 地址。 |
| `DEEPSEEK_MODEL` | `deepseek-v4-flash` | 默认模型。 |
| `DEEPSEEK_TIMEOUT_SECONDS` | `30` | 单次模型请求超时时间。 |

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

- 补充更细粒度的 Agent 调用失败原因、耗时和成本统计。
- 增加可配置提示词模板和面试场景模板。
- 引入更完整的评测集，验证模型输出相关度和稳定性。
