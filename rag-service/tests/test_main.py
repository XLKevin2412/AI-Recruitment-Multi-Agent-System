import importlib
import os
import unittest


def load_app():
    os.environ["RAG_STORAGE_BACKEND"] = "memory"
    module = importlib.import_module("app.main")
    return importlib.reload(module)


class RagServiceTests(unittest.TestCase):

    def test_index_and_search_with_filters(self):
        main = load_app()

        response = main.index_document(
            main.RagDocumentRequest(
                sourceType="RESUME",
                sourceId="resume-1",
                applicationId="application-1",
                jobPositionId="job-1",
                content="Java Spring Boot MySQL Redis project experience",
            )
        )

        self.assertEqual("INDEXED", response.status)
        self.assertEqual(1, response.chunkCount)

        search_response = main.search(
            main.RagSearchRequest(
                query="Spring Boot Redis",
                sourceTypes=["RESUME"],
                topK=3,
                filters={"applicationId": "application-1", "jobPositionId": "job-1"},
            )
        )

        self.assertEqual(1, len(search_response.items))
        self.assertEqual("resume-1", search_response.items[0].sourceId)

    def test_milvus_expression_escapes_values(self):
        main = load_app()

        expression = main.build_milvus_expr(
            ["RESUME", "JOB_REQUIREMENT"],
            {"applicationId": 'app-"1"', "jobPositionId": "job-1"},
        )

        self.assertIn('source_type in ["RESUME", "JOB_REQUIREMENT"]', expression)
        self.assertIn('application_id == "app-\\"1\\""', expression)
        self.assertIn('job_position_id == "job-1"', expression)


if __name__ == "__main__":
    unittest.main()
