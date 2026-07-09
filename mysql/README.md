# MySQL 工作区

本工作区用于管理业务数据库初始化脚本、表结构和后续迁移脚本。

## 当前状态

V1 表结构已覆盖岗位、候选人、申请、简历、分析报告、邮件草稿、面试建议、Agent 调用记录和 RAG 文档元数据。

## 数据库

默认数据库：

```text
ai_recruitment
```

默认字符集：

```text
utf8mb4
utf8mb4_unicode_ci
```

## 初始化脚本

当前脚本：

```text
init/create-database.sql
init/create-tables.sql
init/seed-job-templates.sql
```

脚本职责：

```text
create-database.sql       创建数据库和字符集
create-tables.sql         创建 V1 业务表和索引
seed-job-templates.sql    写入计算机行业岗位模板
```

MySQL 容器首次初始化时会按文件名字母顺序执行这些脚本。

## 主要业务表

- `candidates`：候选人信息。
- `job_templates`：岗位模板。
- `job_positions`：招聘岗位。
- `resumes`：简历文件和解析结果。
- `applications`：候选人申请流程。
- `resume_analysis_reports`：AI 简历分析报告。
- `email_drafts`：邮件草稿。
- `interview_plans`：面试建议。
- `agent_runs`：Agent 调用记录。
- `rag_documents`：RAG 文档元数据。
- `human_review_records`：人工审核记录。

## 健康检查

```sql
SELECT 1;
```

或使用 Docker：

```powershell
docker exec ai-recruitment-mysql mysqladmin ping -h 127.0.0.1 -uroot -p
```
