package com.example.recruitment.template;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job-templates")
public class JobTemplateController {

    @GetMapping
    public List<JobTemplate> list() {
        return List.of(
                new JobTemplate(
                        "java-backend-intern",
                        "Java Backend Intern",
                        "Engineering",
                        List.of("Java", "Spring Boot", "MySQL", "Redis", "REST API"),
                        "Support recruitment system backend feature development and maintenance.",
                        "Understand Java basics, Spring Boot, relational database design, and API debugging."),
                new JobTemplate(
                        "ai-application-engineer",
                        "AI Application Engineer",
                        "AI Platform",
                        List.of("Python", "RAG", "LLM API", "Vector Database", "Prompt Engineering"),
                        "Build AI application workflows around resume analysis and interview assistance.",
                        "Understand LLM integration, retrieval workflows, evaluation, and backend collaboration."),
                new JobTemplate(
                        "hr-operations-specialist",
                        "HR Operations Specialist",
                        "People Operations",
                        List.of("Recruitment Process", "Candidate Communication", "Interview Coordination"),
                        "Coordinate candidate screening, communication, and interview scheduling.",
                        "Familiar with hiring workflows and structured candidate follow-up."));
    }
}
