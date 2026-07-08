from __future__ import annotations

import base64
import io
import json
import os
import re
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any

import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    from pypdf import PdfReader
except Exception:  # pragma: no cover - optional dependency in syntax-only checks
    PdfReader = None


app = FastAPI(title="AI Recruitment Agent Runtime", version="0.1.0")

DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com"
DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-flash"


class ResumeParseRequest(BaseModel):
    traceId: str | None = None
    fileName: str
    fileContentBase64: str


class ResumeParseResponse(BaseModel):
    parsedText: str
    language: str
    parseStatus: str
    errorMessage: str | None = None


class CandidatePayload(BaseModel):
    id: str | None = None
    name: str | None = None
    email: str | None = None


class JobPayload(BaseModel):
    id: str | None = None
    title: str
    requiredSkills: list[str] = Field(default_factory=list)
    preferredSkills: list[str] = Field(default_factory=list)
    experienceRequirement: str | None = None
    description: str | None = None
    interviewRequirements: str | None = None


class ResumePayload(BaseModel):
    id: str | None = None
    parsedText: str


class RagEvidence(BaseModel):
    evidenceId: str
    sourceType: str
    content: str
    score: float


class ResumeAnalysisRequest(BaseModel):
    traceId: str | None = None
    applicationId: str
    candidate: CandidatePayload
    job: JobPayload
    resume: ResumePayload
    ragEvidence: list[RagEvidence] = Field(default_factory=list)


class TokenUsage(BaseModel):
    inputTokens: int
    outputTokens: int
    totalTokens: int


class ResumeAnalysisResponse(BaseModel):
    agentRunId: str
    overallScore: int
    recommendation: str
    matchedSkills: list[str]
    missingSkills: list[str]
    strengths: list[str]
    risks: list[str]
    summary: str
    requiresHumanReview: bool
    modelName: str
    tokenUsage: TokenUsage


class EmailDraftRequest(BaseModel):
    traceId: str | None = None
    applicationId: str
    emailType: str
    candidate: CandidatePayload
    job: JobPayload
    analysisReport: dict[str, Any]


class EmailDraftResponse(BaseModel):
    agentRunId: str
    subject: str
    body: str
    requiresHumanReview: bool
    modelName: str
    tokenUsage: TokenUsage


class TimeSlot(BaseModel):
    startTime: str
    endTime: str


class InterviewPlanRequest(BaseModel):
    traceId: str | None = None
    applicationId: str
    interviewType: str = "TECHNICAL"
    preferredTimezone: str = "Asia/Shanghai"
    candidateAvailableSlots: list[TimeSlot] = Field(default_factory=list)
    job: JobPayload
    analysisReport: dict[str, Any]


class InterviewPlanResponse(BaseModel):
    agentRunId: str
    proposedStartTime: str
    proposedEndTime: str
    timezone: str
    meetingTitle: str
    meetingNotes: str
    requiresHumanReview: bool
    modelName: str
    tokenUsage: TokenUsage


class QuestionAnswerRequest(BaseModel):
    traceId: str | None = None
    question: str
    context: list[RagEvidence] = Field(default_factory=list)


class QuestionAnswerResponse(BaseModel):
    answer: str
    evidenceIds: list[str]
    modelName: str
    tokenUsage: TokenUsage


@app.get("/health")
def health() -> dict[str, str]:
    return {"service": "python-agent-runtime", "status": "UP"}


@app.post("/internal/agents/resume-parse", response_model=ResumeParseResponse)
def parse_resume(request: ResumeParseRequest) -> ResumeParseResponse:
    try:
        raw = base64.b64decode(request.fileContentBase64)
        if request.fileName.lower().endswith(".pdf") and PdfReader is not None:
            parsed = parse_pdf(raw)
        else:
            parsed = raw.decode("utf-8", errors="ignore")
        parsed = re.sub(r"\s+", " ", parsed).strip()
        if not parsed:
            return ResumeParseResponse(parsedText="", language="unknown", parseStatus="FAILED", errorMessage="No text extracted")
        return ResumeParseResponse(parsedText=parsed, language=detect_language(parsed), parseStatus="PARSED")
    except Exception as exc:
        return ResumeParseResponse(parsedText="", language="unknown", parseStatus="FAILED", errorMessage=str(exc))


@app.post("/internal/agents/resume-analysis", response_model=ResumeAnalysisResponse)
def analyze_resume(request: ResumeAnalysisRequest) -> ResumeAnalysisResponse:
    model_response = deepseek_resume_analysis(request)
    if model_response is not None:
        return model_response
    return rule_based_resume_analysis(request)


def rule_based_resume_analysis(request: ResumeAnalysisRequest) -> ResumeAnalysisResponse:
    resume_text = request.resume.parsedText
    matched = [skill for skill in request.job.requiredSkills if contains_skill(resume_text, skill)]
    missing = [skill for skill in request.job.requiredSkills if skill not in matched]
    preferred = [skill for skill in request.job.preferredSkills if contains_skill(resume_text, skill)]
    required_score = 70 if not request.job.requiredSkills else round(70 * len(matched) / len(request.job.requiredSkills))
    preferred_score = min(20, len(preferred) * 5)
    evidence_score = min(10, round(sum(max(item.score, 0) for item in request.ragEvidence[:5]) * 2))
    score = max(0, min(100, required_score + preferred_score + evidence_score))
    recommendation = recommendation_for(score, missing)
    strengths = build_strengths(matched, preferred, request.ragEvidence)
    risks = build_risks(missing, resume_text)
    summary = (
        f"{request.candidate.name or 'Candidate'} matches {len(matched)} required skills "
        f"for {request.job.title}; recommendation is {recommendation}."
    )
    return ResumeAnalysisResponse(
        agentRunId=str(uuid.uuid4()),
        overallScore=score,
        recommendation=recommendation,
        matchedSkills=matched + preferred,
        missingSkills=missing,
        strengths=strengths,
        risks=risks,
        summary=summary,
        requiresHumanReview=True,
        modelName=model_name(),
        tokenUsage=usage_for(resume_text + request.job.title, summary),
    )


@app.post("/internal/agents/email-draft", response_model=EmailDraftResponse)
def draft_email(request: EmailDraftRequest) -> EmailDraftResponse:
    model_response = deepseek_email_draft(request)
    if model_response is not None:
        return model_response
    return rule_based_email_draft(request)


def rule_based_email_draft(request: EmailDraftRequest) -> EmailDraftResponse:
    candidate_name = request.candidate.name or "candidate"
    if request.emailType == "SCREENING_REJECT":
        subject = f"Update on your application for {request.job.title}"
        body = (
            f"Hi {candidate_name},\n\nThank you for applying for {request.job.title}. "
            "After reviewing the current match, we will not move forward at this stage. "
            "We appreciate your time and will keep your profile for suitable future roles.\n"
        )
    else:
        subject = f"Interview invitation for {request.job.title}"
        body = (
            f"Hi {candidate_name},\n\nThank you for applying for {request.job.title}. "
            "Your background appears aligned with the role. Please confirm your availability "
            "for the next interview step.\n"
        )
    return EmailDraftResponse(
        agentRunId=str(uuid.uuid4()),
        subject=subject,
        body=body,
        requiresHumanReview=True,
        modelName=model_name(),
        tokenUsage=usage_for(str(request.model_dump()), subject + body),
    )


@app.post("/internal/agents/interview-plan", response_model=InterviewPlanResponse)
def plan_interview(request: InterviewPlanRequest) -> InterviewPlanResponse:
    model_response = deepseek_interview_plan(request)
    if model_response is not None:
        return model_response
    return rule_based_interview_plan(request)


def rule_based_interview_plan(request: InterviewPlanRequest) -> InterviewPlanResponse:
    if request.candidateAvailableSlots:
        slot = request.candidateAvailableSlots[0]
        start = slot.startTime
        end = slot.endTime
    else:
        start_dt = datetime.now(timezone.utc) + timedelta(days=2)
        end_dt = start_dt + timedelta(hours=1)
        start = start_dt.isoformat().replace("+00:00", "Z")
        end = end_dt.isoformat().replace("+00:00", "Z")
    risks = request.analysisReport.get("risks") or []
    notes = f"Focus on {request.job.interviewRequirements or request.job.title}."
    if risks:
        notes += " Follow up on: " + ", ".join(str(risk) for risk in risks[:3]) + "."
    return InterviewPlanResponse(
        agentRunId=str(uuid.uuid4()),
        proposedStartTime=start,
        proposedEndTime=end,
        timezone=request.preferredTimezone,
        meetingTitle=f"{request.job.title} {request.interviewType.title()} Interview",
        meetingNotes=notes,
        requiresHumanReview=True,
        modelName=model_name(),
        tokenUsage=usage_for(str(request.model_dump()), notes),
    )


@app.post("/internal/agents/qa", response_model=QuestionAnswerResponse)
def answer_question(request: QuestionAnswerRequest) -> QuestionAnswerResponse:
    model_response = deepseek_answer_question(request)
    if model_response is not None:
        return model_response
    return rule_based_answer_question(request)


def rule_based_answer_question(request: QuestionAnswerRequest) -> QuestionAnswerResponse:
    evidence_ids = [item.evidenceId for item in request.context]
    context_text = " ".join(item.content for item in request.context[:3])
    answer = f"Based on the available recruitment context: {context_text}" if context_text else "No supporting context was found."
    return QuestionAnswerResponse(
        answer=answer,
        evidenceIds=evidence_ids,
        modelName=model_name(),
        tokenUsage=usage_for(request.question + context_text, answer),
    )


def deepseek_resume_analysis(request: ResumeAnalysisRequest) -> ResumeAnalysisResponse | None:
    result = invoke_deepseek_json(
        task="resume-analysis",
        system_prompt=(
            "You are a recruitment resume analysis agent. Return only valid json with "
            "overallScore, recommendation, matchedSkills, missingSkills, strengths, "
            "risks, summary, and requiresHumanReview."
        ),
        payload={
            "candidate": request.candidate.model_dump(),
            "job": request.job.model_dump(),
            "resume": request.resume.model_dump(),
            "ragEvidence": [item.model_dump() for item in request.ragEvidence[:8]],
        },
        max_tokens=1200,
    )
    if result is None:
        return None
    data, token_usage = result
    try:
        score = clamp_int(data.get("overallScore"), 0, 100)
        recommendation = normalize_recommendation(data.get("recommendation"), score)
        return ResumeAnalysisResponse(
            agentRunId=str(uuid.uuid4()),
            overallScore=score,
            recommendation=recommendation,
            matchedSkills=string_list(data.get("matchedSkills")),
            missingSkills=string_list(data.get("missingSkills")),
            strengths=string_list(data.get("strengths")) or ["Model identified relevant candidate strengths"],
            risks=string_list(data.get("risks")) or ["No major risk found by model evaluation"],
            summary=string_value(data.get("summary"), "DeepSeek generated a recruitment analysis summary."),
            requiresHumanReview=bool_value(data.get("requiresHumanReview"), True),
            modelName=deepseek_model(),
            tokenUsage=token_usage,
        )
    except Exception:
        return None


def deepseek_email_draft(request: EmailDraftRequest) -> EmailDraftResponse | None:
    result = invoke_deepseek_json(
        task="email-draft",
        system_prompt=(
            "You are a recruitment communication agent. Return only valid json with "
            "subject, body, and requiresHumanReview. The email must be polite and must "
            "not claim that an email has already been sent."
        ),
        payload=request.model_dump(),
        max_tokens=900,
    )
    if result is None:
        return None
    data, token_usage = result
    try:
        subject = string_value(data.get("subject"), "")
        body = string_value(data.get("body"), "")
        if not subject or not body:
            return None
        return EmailDraftResponse(
            agentRunId=str(uuid.uuid4()),
            subject=subject,
            body=body,
            requiresHumanReview=bool_value(data.get("requiresHumanReview"), True),
            modelName=deepseek_model(),
            tokenUsage=token_usage,
        )
    except Exception:
        return None


def deepseek_interview_plan(request: InterviewPlanRequest) -> InterviewPlanResponse | None:
    result = invoke_deepseek_json(
        task="interview-plan",
        system_prompt=(
            "You are a recruitment scheduling agent. Return only valid json with "
            "proposedStartTime, proposedEndTime, timezone, meetingTitle, meetingNotes, "
            "and requiresHumanReview. Do not claim that a calendar event or meeting link "
            "has been created."
        ),
        payload=request.model_dump(),
        max_tokens=900,
    )
    if result is None:
        return None
    data, token_usage = result
    try:
        start = string_value(data.get("proposedStartTime"), "")
        end = string_value(data.get("proposedEndTime"), "")
        title = string_value(data.get("meetingTitle"), "")
        notes = string_value(data.get("meetingNotes"), "")
        if not start or not end or not title or not notes:
            return None
        return InterviewPlanResponse(
            agentRunId=str(uuid.uuid4()),
            proposedStartTime=start,
            proposedEndTime=end,
            timezone=string_value(data.get("timezone"), request.preferredTimezone),
            meetingTitle=title,
            meetingNotes=notes,
            requiresHumanReview=bool_value(data.get("requiresHumanReview"), True),
            modelName=deepseek_model(),
            tokenUsage=token_usage,
        )
    except Exception:
        return None


def deepseek_answer_question(request: QuestionAnswerRequest) -> QuestionAnswerResponse | None:
    result = invoke_deepseek_json(
        task="recruitment-qa",
        system_prompt=(
            "You are a recruitment QA agent. Return only valid json with answer and "
            "evidenceIds. Use only the provided context. If context is insufficient, say so."
        ),
        payload=request.model_dump(),
        max_tokens=700,
    )
    if result is None:
        return None
    data, token_usage = result
    try:
        answer = string_value(data.get("answer"), "")
        if not answer:
            return None
        fallback_evidence_ids = [item.evidenceId for item in request.context]
        evidence_ids = string_list(data.get("evidenceIds")) or fallback_evidence_ids
        return QuestionAnswerResponse(
            answer=answer,
            evidenceIds=evidence_ids,
            modelName=deepseek_model(),
            tokenUsage=token_usage,
        )
    except Exception:
        return None


def invoke_deepseek_json(
        task: str,
        system_prompt: str,
        payload: dict[str, Any],
        max_tokens: int) -> tuple[dict[str, Any], TokenUsage] | None:
    api_key = os.getenv("DEEPSEEK_API_KEY", "").strip()
    if not api_key:
        return None

    user_content = json.dumps({"task": task, "input": payload}, ensure_ascii=False)
    request_body = {
        "model": deepseek_model(),
        "messages": [
            {"role": "system", "content": system_prompt + " The response must be json."},
            {"role": "user", "content": user_content},
        ],
        "response_format": {"type": "json_object"},
        "stream": False,
        "max_tokens": max_tokens,
    }
    try:
        with httpx.Client(timeout=deepseek_timeout_seconds()) as client:
            response = client.post(
                deepseek_chat_url(),
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json=request_body,
            )
            response.raise_for_status()
            raw_response = response.json()
        content = raw_response["choices"][0]["message"]["content"]
        if not content:
            return None
        parsed = json.loads(content)
        if not isinstance(parsed, dict):
            return None
        return parsed, token_usage_from(raw_response.get("usage"), user_content, content)
    except (httpx.HTTPError, KeyError, TypeError, ValueError, json.JSONDecodeError):
        return None


def parse_pdf(raw: bytes) -> str:
    reader = PdfReader(io.BytesIO(raw))
    pages = [page.extract_text() or "" for page in reader.pages]
    return "\n".join(pages)


def detect_language(text: str) -> str:
    chinese_chars = len(re.findall(r"[\u4e00-\u9fff]", text))
    return "zh-CN" if chinese_chars > max(3, len(text) * 0.1) else "en-US"


def contains_skill(text: str, skill: str) -> bool:
    return skill.lower() in text.lower()


def recommendation_for(score: int, missing: list[str]) -> str:
    if score >= 85 and not missing:
        return "STRONG_MATCH"
    if score >= 70:
        return "MATCH"
    if score >= 50:
        return "WEAK_MATCH"
    if missing:
        return "NEEDS_REVIEW"
    return "NOT_MATCH"


def build_strengths(matched: list[str], preferred: list[str], evidence: list[RagEvidence]) -> list[str]:
    strengths: list[str] = []
    if matched:
        strengths.append("Required skills matched: " + ", ".join(matched))
    if preferred:
        strengths.append("Preferred skills present: " + ", ".join(preferred))
    if evidence:
        strengths.append("RAG evidence supports the evaluation")
    return strengths or ["Resume contains relevant experience for review"]


def build_risks(missing: list[str], resume_text: str) -> list[str]:
    risks = ["Missing required skills: " + ", ".join(missing)] if missing else []
    if len(resume_text) < 300:
        risks.append("Resume text is short; manual review is recommended")
    return risks or ["No major risk found by rule-based evaluation"]


def model_name() -> str:
    return "rule-based-agent"


def deepseek_model() -> str:
    return os.getenv("DEEPSEEK_MODEL") or DEFAULT_DEEPSEEK_MODEL


def deepseek_base_url() -> str:
    return os.getenv("DEEPSEEK_BASE_URL") or DEFAULT_DEEPSEEK_BASE_URL


def deepseek_chat_url() -> str:
    return deepseek_base_url().rstrip("/") + "/chat/completions"


def deepseek_timeout_seconds() -> float:
    try:
        return float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "30"))
    except ValueError:
        return 30.0


def token_usage_from(raw_usage: Any, input_text: str, output_text: str) -> TokenUsage:
    if isinstance(raw_usage, dict):
        input_tokens = int(raw_usage.get("prompt_tokens") or raw_usage.get("input_tokens") or 0)
        output_tokens = int(raw_usage.get("completion_tokens") or raw_usage.get("output_tokens") or 0)
        total_tokens = int(raw_usage.get("total_tokens") or input_tokens + output_tokens)
        if total_tokens > 0:
            return TokenUsage(inputTokens=input_tokens, outputTokens=output_tokens, totalTokens=total_tokens)
    return usage_for(input_text, output_text)


def string_value(value: Any, fallback: str) -> str:
    if isinstance(value, str) and value.strip():
        return value.strip()
    if value is None:
        return fallback
    text = str(value).strip()
    return text or fallback


def string_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    result: list[str] = []
    for item in value:
        text = string_value(item, "")
        if text:
            result.append(text)
    return result


def bool_value(value: Any, fallback: bool) -> bool:
    if isinstance(value, bool):
        return value
    return fallback


def clamp_int(value: Any, minimum: int, maximum: int) -> int:
    try:
        number = int(value)
    except (TypeError, ValueError):
        number = minimum
    return max(minimum, min(maximum, number))


def normalize_recommendation(value: Any, score: int) -> str:
    allowed = {"STRONG_MATCH", "MATCH", "WEAK_MATCH", "NEEDS_REVIEW", "NOT_MATCH"}
    text = string_value(value, "").upper()
    if text in allowed:
        return text
    return recommendation_for(score, [])


def usage_for(input_text: str, output_text: str) -> TokenUsage:
    input_tokens = max(1, len(input_text) // 4)
    output_tokens = max(1, len(output_text) // 4)
    return TokenUsage(inputTokens=input_tokens, outputTokens=output_tokens, totalTokens=input_tokens + output_tokens)
