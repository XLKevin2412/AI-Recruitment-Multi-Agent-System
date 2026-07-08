from __future__ import annotations

import hashlib
import json
import math
import os
import re
import uuid
from abc import ABC, abstractmethod
from collections import Counter
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    from pymilvus import Collection, CollectionSchema, DataType, FieldSchema, connections, utility
except Exception:  # pragma: no cover - pymilvus may be absent in lightweight local checks
    Collection = None
    CollectionSchema = None
    DataType = None
    FieldSchema = None
    connections = None
    utility = None


app = FastAPI(title="AI Recruitment RAG Service", version="0.1.0")

TOKEN_PATTERN = re.compile(r"[A-Za-z0-9_+#.-]+|[\u4e00-\u9fff]")
VECTOR_DIMENSION = int(os.getenv("EMBEDDING_DIMENSION", "128"))
CHUNK_SIZE = int(os.getenv("RAG_CHUNK_SIZE", "700"))
CHUNK_OVERLAP = int(os.getenv("RAG_CHUNK_OVERLAP", "120"))
STORAGE_BACKEND = os.getenv("RAG_STORAGE_BACKEND", "auto").lower()
MILVUS_HOST = os.getenv("MILVUS_HOST", "localhost")
MILVUS_PORT = os.getenv("MILVUS_GRPC_PORT", "19530")
MILVUS_COLLECTION = os.getenv("MILVUS_COLLECTION", "recruitment_knowledge_chunks")


class RagDocumentRequest(BaseModel):
    traceId: str | None = None
    sourceType: str
    sourceId: str
    applicationId: str | None = None
    jobPositionId: str | None = None
    content: str = Field(min_length=1)
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagDocumentResponse(BaseModel):
    documentId: str
    chunkCount: int
    status: str


class RagSearchRequest(BaseModel):
    traceId: str | None = None
    query: str = Field(min_length=1)
    sourceTypes: list[str] = Field(default_factory=list)
    topK: int = Field(default=5, ge=1, le=50)
    filters: dict[str, str | None] = Field(default_factory=dict)


class RagSearchItem(BaseModel):
    evidenceId: str
    sourceType: str
    sourceId: str
    chunkId: str
    content: str
    score: float
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagSearchResponse(BaseModel):
    items: list[RagSearchItem]


class StoredChunk(BaseModel):
    evidenceId: str
    documentId: str
    sourceType: str
    sourceId: str
    applicationId: str | None
    jobPositionId: str | None
    chunkId: str
    content: str
    embedding: list[float]
    metadata: dict[str, Any]
    createdAt: str


class ChunkStore(ABC):

    @abstractmethod
    def index(self, stored_chunks: list[StoredChunk]) -> None:
        raise NotImplementedError

    @abstractmethod
    def search(
            self,
            query_embedding: list[float],
            source_types: list[str],
            filters: dict[str, str | None],
            top_k: int) -> list[tuple[float, StoredChunk]]:
        raise NotImplementedError

    @abstractmethod
    def status(self) -> dict[str, str]:
        raise NotImplementedError


class MemoryChunkStore(ChunkStore):

    def __init__(self, reason: str | None = None) -> None:
        self.chunks: list[StoredChunk] = []
        self.reason = reason

    def index(self, stored_chunks: list[StoredChunk]) -> None:
        self.chunks.extend(stored_chunks)

    def search(
            self,
            query_embedding: list[float],
            source_types: list[str],
            filters: dict[str, str | None],
            top_k: int) -> list[tuple[float, StoredChunk]]:
        ranked: list[tuple[float, StoredChunk]] = []
        for chunk in self.chunks:
            if source_types and chunk.sourceType not in source_types:
                continue
            if not matches_filters(chunk, filters):
                continue
            ranked.append((cosine_similarity(query_embedding, chunk.embedding), chunk))
        ranked.sort(key=lambda item: item[0], reverse=True)
        return ranked[:top_k]

    def status(self) -> dict[str, str]:
        body = {"backend": "memory", "status": "UP", "chunks": str(len(self.chunks))}
        if self.reason:
            body["reason"] = self.reason
        return body


class MilvusChunkStore(ChunkStore):

    def __init__(self) -> None:
        if Collection is None or connections is None or utility is None:
            raise RuntimeError("pymilvus is not available")
        self.alias = "rag_service"
        connections.connect(alias=self.alias, host=MILVUS_HOST, port=MILVUS_PORT)
        self.collection = self.ensure_collection()
        self.collection.load()

    def ensure_collection(self) -> Any:
        if utility.has_collection(MILVUS_COLLECTION, using=self.alias):
            return Collection(MILVUS_COLLECTION, using=self.alias)

        fields = [
            FieldSchema(name="evidence_id", dtype=DataType.VARCHAR, is_primary=True, max_length=64),
            FieldSchema(name="document_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="source_type", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="source_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="application_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="job_position_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="chunk_id", dtype=DataType.VARCHAR, max_length=128),
            FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=8192),
            FieldSchema(name="metadata_json", dtype=DataType.VARCHAR, max_length=4096),
            FieldSchema(name="created_at", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=VECTOR_DIMENSION),
        ]
        schema = CollectionSchema(fields=fields, description="AI recruitment RAG chunks")
        collection = Collection(MILVUS_COLLECTION, schema=schema, using=self.alias)
        collection.create_index(
            field_name="embedding",
            index_params={"index_type": "HNSW", "metric_type": "COSINE", "params": {"M": 8, "efConstruction": 64}},
        )
        return collection

    def index(self, stored_chunks: list[StoredChunk]) -> None:
        if not stored_chunks:
            return
        entities = [
            {
                "evidence_id": chunk.evidenceId,
                "document_id": chunk.documentId,
                "source_type": chunk.sourceType,
                "source_id": chunk.sourceId,
                "application_id": chunk.applicationId or "",
                "job_position_id": chunk.jobPositionId or "",
                "chunk_id": chunk.chunkId,
                "content": chunk.content,
                "metadata_json": json.dumps(chunk.metadata, ensure_ascii=False),
                "created_at": chunk.createdAt,
                "embedding": chunk.embedding,
            }
            for chunk in stored_chunks
        ]
        self.collection.insert(entities)
        self.collection.flush()

    def search(
            self,
            query_embedding: list[float],
            source_types: list[str],
            filters: dict[str, str | None],
            top_k: int) -> list[tuple[float, StoredChunk]]:
        expression = build_milvus_expr(source_types, filters)
        results = self.collection.search(
            data=[query_embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"ef": 64}},
            limit=top_k,
            expr=expression or None,
            output_fields=[
                "evidence_id",
                "document_id",
                "source_type",
                "source_id",
                "application_id",
                "job_position_id",
                "chunk_id",
                "content",
                "metadata_json",
                "created_at",
            ],
        )
        ranked: list[tuple[float, StoredChunk]] = []
        for hit in results[0]:
            entity = hit.entity
            metadata_raw = entity.get("metadata_json") or "{}"
            try:
                metadata = json.loads(metadata_raw)
            except json.JSONDecodeError:
                metadata = {}
            chunk = StoredChunk(
                evidenceId=entity.get("evidence_id"),
                documentId=entity.get("document_id"),
                sourceType=entity.get("source_type"),
                sourceId=entity.get("source_id"),
                applicationId=entity.get("application_id") or None,
                jobPositionId=entity.get("job_position_id") or None,
                chunkId=entity.get("chunk_id"),
                content=entity.get("content"),
                embedding=[],
                metadata=metadata,
                createdAt=entity.get("created_at"),
            )
            ranked.append((float(hit.score), chunk))
        return ranked

    def status(self) -> dict[str, str]:
        return {
            "backend": "milvus",
            "status": "UP",
            "host": MILVUS_HOST,
            "port": MILVUS_PORT,
            "collection": MILVUS_COLLECTION,
        }


def create_store() -> ChunkStore:
    if STORAGE_BACKEND == "memory":
        return MemoryChunkStore()
    try:
        return MilvusChunkStore()
    except Exception as exc:
        if STORAGE_BACKEND == "milvus":
            raise
        return MemoryChunkStore(reason=f"Milvus unavailable: {exc}")


store: ChunkStore = create_store()


@app.get("/health")
def health() -> dict[str, str]:
    return {"service": "rag-service", **store.status()}


@app.post("/internal/rag/documents", response_model=RagDocumentResponse)
def index_document(request: RagDocumentRequest) -> RagDocumentResponse:
    document_id = str(uuid.uuid4())
    text_chunks = split_text(request.content)
    now = datetime.now(timezone.utc).isoformat()
    stored_chunks: list[StoredChunk] = []
    for index, content in enumerate(text_chunks):
        chunk_id = f"{document_id}:{index}"
        stored_chunks.append(
            StoredChunk(
                evidenceId=str(uuid.uuid4()),
                documentId=document_id,
                sourceType=request.sourceType,
                sourceId=request.sourceId,
                applicationId=request.applicationId,
                jobPositionId=request.jobPositionId,
                chunkId=chunk_id,
                content=content,
                embedding=embed(content),
                metadata={**request.metadata, "chunkIndex": index},
                createdAt=now,
            )
        )
    store.index(stored_chunks)
    return RagDocumentResponse(documentId=document_id, chunkCount=len(text_chunks), status="INDEXED")


@app.post("/internal/rag/search", response_model=RagSearchResponse)
def search(request: RagSearchRequest) -> RagSearchResponse:
    query_embedding = embed(request.query)
    ranked = store.search(query_embedding, request.sourceTypes, request.filters, request.topK)
    items = [
        RagSearchItem(
            evidenceId=chunk.evidenceId,
            sourceType=chunk.sourceType,
            sourceId=chunk.sourceId,
            chunkId=chunk.chunkId,
            content=chunk.content,
            score=round(score, 4),
            metadata=chunk.metadata,
        )
        for score, chunk in ranked
    ]
    return RagSearchResponse(items=items)


def split_text(text: str) -> list[str]:
    normalized = re.sub(r"\s+", " ", text).strip()
    if len(normalized) <= CHUNK_SIZE:
        return [normalized]

    result: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(len(normalized), start + CHUNK_SIZE)
        result.append(normalized[start:end])
        if end == len(normalized):
            break
        start = max(end - CHUNK_OVERLAP, start + 1)
    return result


def embed(text: str) -> list[float]:
    tokens = tokenize(text)
    vector = [0.0] * VECTOR_DIMENSION
    counts = Counter(tokens)
    for token, count in counts.items():
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:4], "big") % VECTOR_DIMENSION
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        vector[index] += sign * (1.0 + math.log(count))
    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector
    return [value / norm for value in vector]


def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_PATTERN.findall(text)]


def cosine_similarity(left: list[float], right: list[float]) -> float:
    return sum(left_value * right_value for left_value, right_value in zip(left, right))


def matches_filters(chunk: StoredChunk, filters: dict[str, str | None]) -> bool:
    for key, expected in filters.items():
        if expected in (None, ""):
            continue
        if key == "applicationId" and chunk.applicationId != expected:
            return False
        if key == "jobPositionId" and chunk.jobPositionId != expected:
            return False
    return True


def build_milvus_expr(source_types: list[str], filters: dict[str, str | None]) -> str:
    expressions: list[str] = []
    if source_types:
        values = ", ".join(f'"{escape_milvus_string(source_type)}"' for source_type in source_types)
        expressions.append(f"source_type in [{values}]")
    for key, expected in filters.items():
        if expected in (None, ""):
            continue
        if key == "applicationId":
            expressions.append(f'application_id == "{escape_milvus_string(expected)}"')
        if key == "jobPositionId":
            expressions.append(f'job_position_id == "{escape_milvus_string(expected)}"')
    return " and ".join(expressions)


def escape_milvus_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')
