# Python Agent Runtime

FastAPI service for AI recruitment agent capabilities.

## Endpoints

- `GET /health`
- `POST /internal/agents/resume-parse`
- `POST /internal/agents/resume-analysis`
- `POST /internal/agents/email-draft`
- `POST /internal/agents/interview-plan`
- `POST /internal/agents/qa`

The service returns structured JSON for Java Backend. If `DEEPSEEK_API_KEY` and `DEEPSEEK_MODEL` are not configured, it uses deterministic rule-based output so the local workflow can be demonstrated without external LLM credentials.

## Run

```powershell
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8100
```
