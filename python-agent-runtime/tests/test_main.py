import importlib
import json
import os
import unittest
from unittest.mock import patch


def load_app(api_key: str = ""):
    os.environ["DEEPSEEK_API_KEY"] = api_key
    os.environ["DEEPSEEK_MODEL"] = "deepseek-v4-flash"
    module = importlib.import_module("app.main")
    return importlib.reload(module)


class FakeDeepSeekResponse:

    def __init__(self, content: str):
        self.content = content

    def raise_for_status(self):
        return None

    def json(self):
        return {
            "choices": [{"message": {"content": self.content}}],
            "usage": {"prompt_tokens": 11, "completion_tokens": 7, "total_tokens": 18},
        }


class FakeDeepSeekClient:

    def __init__(self, responses: list[str]):
        self.responses = responses

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False

    def post(self, url, headers, json):
        return FakeDeepSeekResponse(self.responses.pop(0))


class AgentRuntimeTests(unittest.TestCase):

    def test_missing_api_key_uses_rule_based_fallback(self):
        main = load_app(api_key="")
        response = main.analyze_resume(sample_analysis_request(main))

        self.assertEqual("rule-based-agent", response.modelName)
        self.assertIn("Java", response.matchedSkills)

    def test_deepseek_json_success_is_used_by_agent_endpoints(self):
        main = load_app(api_key="test-key")
        fake_client = FakeDeepSeekClient([
            json.dumps({
                "overallScore": 88,
                "recommendation": "STRONG_MATCH",
                "matchedSkills": ["Java", "Spring Boot"],
                "missingSkills": [],
                "strengths": ["Strong backend experience"],
                "risks": [],
                "summary": "Candidate is a strong fit.",
                "requiresHumanReview": True,
            }),
            json.dumps({
                "subject": "Interview invitation",
                "body": "Please confirm your availability.",
                "requiresHumanReview": True,
            }),
            json.dumps({
                "proposedStartTime": "2026-07-10T09:00:00Z",
                "proposedEndTime": "2026-07-10T10:00:00Z",
                "timezone": "Asia/Shanghai",
                "meetingTitle": "Java Backend Interview",
                "meetingNotes": "Focus on Spring Boot and Redis.",
                "requiresHumanReview": True,
            }),
            json.dumps({
                "answer": "The candidate matches the Java backend role.",
                "evidenceIds": ["evidence-1"],
            }),
        ])

        with patch.object(main.httpx, "Client", return_value=fake_client):
            analysis = main.analyze_resume(sample_analysis_request(main))
            email = main.draft_email(sample_email_request(main))
            interview = main.plan_interview(sample_interview_request(main))
            qa = main.answer_question(sample_qa_request(main))

        self.assertEqual("deepseek-v4-flash", analysis.modelName)
        self.assertEqual(88, analysis.overallScore)
        self.assertEqual("Interview invitation", email.subject)
        self.assertEqual("2026-07-10T09:00:00Z", interview.proposedStartTime)
        self.assertEqual(["evidence-1"], qa.evidenceIds)

    def test_invalid_deepseek_json_falls_back_without_breaking_endpoint(self):
        main = load_app(api_key="test-key")
        fake_client = FakeDeepSeekClient(["{not valid json"])

        with patch.object(main.httpx, "Client", return_value=fake_client):
            response = main.analyze_resume(sample_analysis_request(main))

        self.assertEqual("rule-based-agent", response.modelName)
        self.assertIn("Java", response.matchedSkills)


def sample_analysis_request(main):
    return main.ResumeAnalysisRequest(
        applicationId="application-1",
        candidate=main.CandidatePayload(id="candidate-1", name="Test Candidate", email="test@example.com"),
        job=main.JobPayload(
            id="job-1",
            title="Java Backend Engineer",
            requiredSkills=["Java", "Spring Boot"],
            preferredSkills=["Redis"],
        ),
        resume=main.ResumePayload(id="resume-1", parsedText="Java Spring Boot Redis project experience"),
        ragEvidence=[
            main.RagEvidence(
                evidenceId="evidence-1",
                sourceType="RESUME",
                content="Java Spring Boot Redis project experience",
                score=0.91,
            )
        ],
    )


def sample_email_request(main):
    return main.EmailDraftRequest(
        applicationId="application-1",
        emailType="INTERVIEW_INVITATION",
        candidate=main.CandidatePayload(id="candidate-1", name="Test Candidate", email="test@example.com"),
        job=main.JobPayload(id="job-1", title="Java Backend Engineer"),
        analysisReport={"overallScore": 88, "recommendation": "STRONG_MATCH"},
    )


def sample_interview_request(main):
    return main.InterviewPlanRequest(
        applicationId="application-1",
        job=main.JobPayload(id="job-1", title="Java Backend Engineer"),
        analysisReport={"risks": []},
    )


def sample_qa_request(main):
    return main.QuestionAnswerRequest(
        question="Is this candidate suitable?",
        context=[
            main.RagEvidence(
                evidenceId="evidence-1",
                sourceType="RESUME",
                content="Java Spring Boot Redis project experience",
                score=0.91,
            )
        ],
    )


if __name__ == "__main__":
    unittest.main()
