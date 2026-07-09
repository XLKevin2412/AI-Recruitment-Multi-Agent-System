package com.example.recruitment.integration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IntegrationClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void agentRuntimeClientSendsJsonRequestBody() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://agent-runtime");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AgentRuntimeClient client = new AgentRuntimeClient(builder.build(), objectMapper);

        server.expect(requestTo("http://agent-runtime/internal/agents/resume-parse"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "traceId": "trace-1",
                          "fileName": "resume.pdf",
                          "fileContentBase64": "aGVsbG8="
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "parsedText": "hello",
                          "language": "en-US",
                          "parseStatus": "PARSED",
                          "errorMessage": null
                        }
                        """, MediaType.APPLICATION_JSON));

        AgentRuntimeClient.ResumeParseResponse response = client.parseResume(
                new AgentRuntimeClient.ResumeParseRequest("trace-1", "resume.pdf", "aGVsbG8="));

        assertThat(response.parseStatus(), is("PARSED"));
        assertThat(response.parsedText(), is("hello"));
        server.verify();
    }

    @Test
    void ragServiceClientSendsJsonRequestBody() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://rag-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RagServiceClient client = new RagServiceClient(builder.build(), objectMapper);

        server.expect(requestTo("http://rag-service/internal/rag/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "traceId": "trace-1",
                          "query": "Python FastAPI",
                          "sourceTypes": ["RESUME"],
                          "topK": 3,
                          "filters": {
                            "applicationId": "app-1"
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "items": []
                        }
                        """, MediaType.APPLICATION_JSON));

        RagServiceClient.RagSearchResponse response = client.search(
                new RagServiceClient.RagSearchRequest(
                        "trace-1",
                        "Python FastAPI",
                        List.of("RESUME"),
                        3,
                        Map.of("applicationId", "app-1")));

        assertThat(response.items().isEmpty(), is(true));
        server.verify();
    }
}
