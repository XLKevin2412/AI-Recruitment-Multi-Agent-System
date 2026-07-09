# Redis 工作区

本工作区用于记录 Redis 使用场景、key 命名规范、TTL 建议和健康检查方式。

## 当前状态

当前 Java 后端已使用 Redis 缓存岗位查询结果和最新分析结果；其他任务状态、限流和检索缓存场景保留为后续扩展方向。

## 使用场景

V1 使用 Redis 保存短期、高频、可过期的数据：

- 幂等锁。
- Agent 任务状态。
- RAG 检索短期缓存。
- DeepSeek 调用短期缓存。
- 接口限流计数。
- 岗位信息和最新分析结果缓存。

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
ai-recruitment:job:{jobId}
ai-recruitment:analysis:latest:{applicationId}
```

## TTL 建议

| 场景 | TTL |
| --- | --- |
| 幂等锁 | 5-30 分钟 |
| Agent 任务状态 | 24 小时 |
| RAG 检索缓存 | 10-60 分钟 |
| DeepSeek 短期缓存 | 10-60 分钟 |
| 岗位信息缓存 | 10-60 分钟 |
| 最新分析结果缓存 | 1 小时 |
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
