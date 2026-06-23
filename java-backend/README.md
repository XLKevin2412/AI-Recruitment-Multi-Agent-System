# AI Recruitment Java Backend

This module is the Spring Boot foundation for the recruitment backend.

## Run locally

Start dependencies:

```powershell
docker compose -f ..\infra\docker-compose.yml up -d
```

Set MySQL and Redis with environment variables, or use the defaults below:

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="ai_recruitment"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

Default base URL: `http://localhost:8080/api`.

## Initial endpoints

- `GET /api/health`
- `GET /api/actuator/health`
- `GET /api/job-templates`
- `GET /api/jobs`, `POST /api/jobs`, `GET /api/jobs/{id}`, `PUT /api/jobs/{id}`, `DELETE /api/jobs/{id}`
- `GET /api/candidates`, `POST /api/candidates`, `GET /api/candidates/{id}`, `PUT /api/candidates/{id}`, `DELETE /api/candidates/{id}`
- `GET /api/applications`, `POST /api/applications`, `GET /api/applications/{id}`, `PUT /api/applications/{id}`, `DELETE /api/applications/{id}`
