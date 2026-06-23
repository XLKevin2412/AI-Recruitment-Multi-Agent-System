# Redis 工作区

本工作区用于记录 Redis 的使用场景、key 命名规范和健康检查方式。

## 使用场景

V1 使用 Redis 保存：

- 幂等锁。
- Agent 任务状态。
- RAG 检索短期缓存。
- DeepSeek 调用短期缓存。
- 接口限流计数。

## Key 命名规范

统一格式：

```text
ai-recruitment:{domain}:{type}:{id}
```

示例：

```text
ai-recruitment:application:lock:{applicationId}
ai-recruitment:agent-run:status:{agentRunId}
ai-recruitment:rag:search-cache:{hash}
ai-recruitment:rate-limit:user:{userId}
```

## TTL 建议

| 场景 | TTL |
| --- | --- |
| 幂等锁 | 5-30 分钟 |
| Agent 任务状态 | 24 小时 |
| RAG 检索缓存 | 10-60 分钟 |
| DeepSeek 短期缓存 | 10-60 分钟 |
| 限流计数 | 1-60 分钟 |

## 健康检查

无密码：

```powershell
docker exec ai-recruitment-redis redis-cli ping
```

有密码：

```powershell
docker exec ai-recruitment-redis redis-cli -a <password> ping
```

成功结果：

```text
PONG
```

