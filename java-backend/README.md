# AI Recruitment Java Backend

This module is the Spring Boot recruitment backend for the V1 hiring workflow.

## Run locally

Start dependencies:

```powershell
docker compose -f ..\infra\docker-compose.yml --env-file ..\infra\.env up -d
```

Set MySQL and Redis with environment variables. If the local `infra\.env` keeps the current development defaults, use:

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="13306"
$env:MYSQL_DATABASE="ai_recruitment"
$env:MYSQL_USERNAME="ai_recruitment"
$env:MYSQL_PASSWORD="change_me"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

Default base URL: `http://localhost:8080/api`.

## Initial endpoints

- `GET /api/health`
- `GET /api/actuator/health`
- `GET /api/job-templates`
- `GET /api/jobs?page=1&pageSize=20`, `POST /api/jobs`, `GET /api/jobs/{id}`, `PUT /api/jobs/{id}`, `DELETE /api/jobs/{id}`
- `GET /api/candidates?page=1&pageSize=20`, `POST /api/candidates`, `GET /api/candidates/{id}`, `PUT /api/candidates/{id}`, `DELETE /api/candidates/{id}`
- `GET /api/applications?page=1&pageSize=20`, `POST /api/applications`, `GET /api/applications/{id}`, `PUT /api/applications/{id}`, `DELETE /api/applications/{id}`
- `POST /api/applications/{id}/resume`
- `POST /api/applications/{id}/analysis`, `GET /api/applications/{id}/analysis`
- `POST /api/applications/{id}/emails/draft`, `GET /api/applications/{id}/emails`
- `POST /api/applications/{id}/interviews/propose`, `GET /api/applications/{id}/interviews`
- `POST /api/applications/{id}/qa`

## Application workflow rules

- `POST /api/applications` only accepts `DRAFT` or `SUBMITTED` as the initial status.
- `PUT /api/applications/{id}` updates status, resume binding, and current stage only.
- Candidate and job bindings are immutable after application creation.
- Invalid status transitions return `409 Conflict`.

## V1 integration notes

- The backend calls Python Agent Runtime through `AGENT_RUNTIME_BASE_URL`.
- The backend calls RAG Service through `RAG_SERVICE_BASE_URL`.
- Resume files are stored under `RESUME_STORAGE_DIR`.
- Redis is used for job lookup and latest analysis cache, but cache failures do not fail business requests.
- Current persistence uses Spring Data JPA. The original target mentioned MyBatis; migrating the data layer can be done later without changing the service API.
