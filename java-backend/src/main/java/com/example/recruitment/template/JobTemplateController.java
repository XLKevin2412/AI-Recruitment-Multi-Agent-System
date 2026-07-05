package com.example.recruitment.template;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job-templates")
public class JobTemplateController {

    private final JobTemplateRepository jobTemplateRepository;

    public JobTemplateController(JobTemplateRepository jobTemplateRepository) {
        this.jobTemplateRepository = jobTemplateRepository;
    }

    @GetMapping
    public Iterable<JobTemplate> list() {
        return jobTemplateRepository.findByActiveTrueOrderByJobTypeAsc();
    }
}
