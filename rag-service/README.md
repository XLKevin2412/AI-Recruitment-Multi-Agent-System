# RAG Service

FastAPI service for document chunking and semantic retrieval.

## Endpoints

- `GET /health`
- `POST /internal/rag/documents`
- `POST /internal/rag/search`

V1 uses deterministic local embeddings and in-process chunk storage. The service boundary and Docker dependencies are prepared for Milvus-backed persistence, so the retrieval implementation can be replaced without changing Java Backend or Agent Runtime contracts.

## Run

```powershell
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8200
```
