package com.example.recruitment;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class AiRecruitmentBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void createApplicationRejectsInvalidInitialStatus() throws Exception {
        String jobId = createJob("Application Reject Job");
        String candidateId = createCandidate("reject");

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "candidateId": "%s",
                                  "status": "INTERVIEW_CONFIRMED",
                                  "currentStage": "INTERVIEW_CONFIRMED"
                                }
                                """.formatted(jobId, candidateId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Application can only be created as DRAFT or SUBMITTED")));
    }

    @Test
    void updateApplicationRejectsInvalidStatusTransition() throws Exception {
        String jobId = createJob("Application Transition Job");
        String candidateId = createCandidate("transition");
        String applicationId = createApplication(jobId, candidateId);

        mockMvc.perform(put("/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INTERVIEW_CONFIRMED",
                                  "currentStage": "INTERVIEW_CONFIRMED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Invalid application status transition: SUBMITTED -> INTERVIEW_CONFIRMED")));
    }

    @Test
    void updateApplicationIgnoresCandidateAndJobFields() throws Exception {
        String firstJobId = createJob("Original Application Job");
        String secondJobId = createJob("Ignored Application Job");
        String firstCandidateId = createCandidate("original");
        String secondCandidateId = createCandidate("ignored");
        String applicationId = createApplication(firstJobId, firstCandidateId);

        mockMvc.perform(put("/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "candidateId": "%s",
                                  "status": "RESUME_PROCESSING",
                                  "currentStage": "RESUME_PROCESSING"
                                }
                                """.formatted(secondJobId, secondCandidateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.id", is(firstJobId)))
                .andExpect(jsonPath("$.candidate.id", is(firstCandidateId)))
                .andExpect(jsonPath("$.status", is("RESUME_PROCESSING")));
    }

    @Test
    void createApplicationRejectsMissingResume() throws Exception {
        String jobId = createJob("Application Resume Job");
        String candidateId = createCandidate("resume");

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "candidateId": "%s",
                                  "resumeId": "missing-resume",
                                  "status": "SUBMITTED",
                                  "currentStage": "SUBMITTED"
                                }
                                """.formatted(jobId, candidateId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Resume not found: missing-resume")));
    }

    @Test
    void uploadResumeRejectsPdfExtensionWithInvalidContent() throws Exception {
        String jobId = createJob("Invalid PDF Job");
        String candidateId = createCandidate("invalid-pdf");
        String applicationId = createApplication(jobId, candidateId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "this is not a PDF".getBytes());

        mockMvc.perform(multipart("/applications/{id}/resume", applicationId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Uploaded file is not a valid PDF")));
    }

    @Test
    void uploadResumeRejectsOversizedFileBeforeParsing() throws Exception {
        String jobId = createJob("Oversized PDF Job");
        String candidateId = createCandidate("oversized-pdf");
        String applicationId = createApplication(jobId, candidateId);
        byte[] content = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(), 0, content, 0, 5);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                content);

        mockMvc.perform(multipart("/applications/{id}/resume", applicationId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Resume file must not exceed 10 MiB")));
    }

    @Test
    void uploadResumeDoesNotTurnLongNonPdfNameIntoPdf() throws Exception {
        String jobId = createJob("Long Filename Job");
        String candidateId = createCandidate("long-filename");
        String applicationId = createApplication(jobId, candidateId);
        String fileName = "a".repeat(260) + ".exe";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "%PDF-1.7".getBytes());

        mockMvc.perform(multipart("/applications/{id}/resume", applicationId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Only PDF resumes are supported")));
    }

    private String createJob(String title) throws Exception {
        String response = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "jobType": "JAVA_BACKEND_ENGINEER",
                                  "department": "Engineering",
                                  "level": "V1",
                                  "requiredSkillsJson": "[\\"Java\\",\\"Spring Boot\\"]",
                                  "status": "ACTIVE"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createCandidate(String emailPrefix) throws Exception {
        String response = mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test Candidate",
                                  "email": "%s@example.com",
                                  "source": "TEST"
                                }
                                """.formatted(emailPrefix + System.nanoTime())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createApplication(String jobId, String candidateId) throws Exception {
        String response = mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "candidateId": "%s",
                                  "status": "SUBMITTED",
                                  "currentStage": "SUBMITTED"
                                }
                                """.formatted(jobId, candidateId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
