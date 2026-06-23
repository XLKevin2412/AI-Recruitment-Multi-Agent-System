# MySQL 工作区

本工作区用于管理业务数据库初始化和后续迁移脚本。

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
init/01-create-database.sql
```

后续建议继续添加：

```text
init/02-create-tables.sql
init/03-seed-job-templates.sql
```

## 健康检查

```sql
SELECT 1;
```

或：

```powershell
docker exec ai-recruitment-mysql mysqladmin ping -h 127.0.0.1 -uroot -p
```

