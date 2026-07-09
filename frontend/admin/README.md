# Vue Admin Frontend

招聘管理端前端，用于演示 V1 招聘闭环：创建岗位、候选人和申请记录，上传简历，触发 Agent 分析，并查看邮件草稿、面试建议和招聘问答结果。

## 本地开发

```powershell
cd frontend/admin
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:3000
```

Vite 开发服务会把 `/api` 代理到 `http://localhost:8080`，因此需要先启动 Java Backend。

## Docker Compose

从 `infra` 目录启动：

```powershell
cd infra
docker compose --env-file .env up -d frontend-admin
```

Nginx 会把 `/api` 反向代理到 Compose 网络内的 `java-backend:8080`。
