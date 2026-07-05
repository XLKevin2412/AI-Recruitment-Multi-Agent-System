from __future__ import annotations

import hashlib
import math
import os
import re
import uuid
from collections import Counter
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field


app = FastAPI(title="AI Recruitment RAG Service", version="0.1.0")

TOKEN_PATTERN = re.compile(r"[A-Za-z0-9_+#.-]+|[\u4e00-\u9fff]")
VECTOR_DIMENSION = int(os.getenv("EMBEDDING_DIMENSION", "128"))
CHUNK_SIZE = int(os.getenv("RAG_CHUNK_SIZE", "700"))
CHUNK_OVERLAP = int(os.getenv("RAG_CHUNK_OVERLAP", "120"))


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


chunks: list[StoredChunk] = []


@app.get("/health")
def health() -> dict[str, str]:
    return {"service": "rag-service", "status": "UP"}


@app.post("/internal/rag/documents", response_model=RagDocumentResponse)
def index_document(request: RagDocumentRequest) -> RagDocumentResponse:
    document_id = str(uuid.uuid4())
    text_chunks = split_text(request.content)
    now = datetime.now(timezone.utc).isoformat()
    for index, content in enumerate(text_chunks):
        chunk_id = f"{document_id}:{index}"
        chunks.append(
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
    return RagDocumentResponse(documentId=document_id, chunkCount=len(text_chunks), status="INDEXED")


@app.post("/internal/rag/search", response_model=RagSearchResponse)
def search(request: RagSearchRequest) -> RagSearchResponse:
    query_embedding = embed(request.query)
    ranked: list[tuple[float, StoredChunk]] = []
    for chunk in chunks:
        if request.sourceTypes and chunk.sourceType not in request.sourceTypes:
            continue
        if not matches_filters(chunk, request.filters):
            continue
        ranked.append((cosine_similarity(query_embedding, chunk.embedding), chunk))

    ranked.sort(key=lambda item: item[0], reverse=True)
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
        for score, chunk in ranked[: request.topK]
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
