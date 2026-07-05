package com.example.recruitment.application;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.example.recruitment.common.BadRequestException;
import com.example.recruitment.common.ConflictException;

import org.springframework.stereotype.Component;

@Component
public class ApplicationStatusTransitionValidator {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DRAFT, EnumSet.of(ApplicationStatus.SUBMITTED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.SUBMITTED, EnumSet.of(ApplicationStatus.RESUME_PROCESSING));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.RESUME_PROCESSING, EnumSet.of(ApplicationStatus.WAITING_ANALYSIS));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.WAITING_ANALYSIS, EnumSet.of(ApplicationStatus.ANALYZING));
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.ANALYZING,
                EnumSet.of(ApplicationStatus.WAITING_HUMAN_REVIEW, ApplicationStatus.FAILED));
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.WAITING_HUMAN_REVIEW,
                EnumSet.of(ApplicationStatus.ANALYSIS_COMPLETED, ApplicationStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(
                ApplicationStatus.ANALYSIS_COMPLETED,
                EnumSet.of(ApplicationStatus.EMAIL_DRAFTED, ApplicationStatus.INTERVIEW_PROPOSED, ApplicationStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.EMAIL_DRAFTED, EnumSet.of(ApplicationStatus.INTERVIEW_PROPOSED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.INTERVIEW_PROPOSED, EnumSet.of(ApplicationStatus.INTERVIEW_CONFIRMED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.INTERVIEW_CONFIRMED, EnumSet.of(ApplicationStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.FAILED, EnumSet.of(ApplicationStatus.WAITING_ANALYSIS));
    }

    public void validate(ApplicationStatus current, ApplicationStatus next) {
        if (current == next) {
            return;
        }
        Set<ApplicationStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedNextStatuses.contains(next)) {
            throw new ConflictException("Invalid application status transition: " + current + " -> " + next);
        }
    }

    public void validateInitial(ApplicationStatus status) {
        if (status != ApplicationStatus.DRAFT && status != ApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Application can only be created as DRAFT or SUBMITTED");
        }
    }
}
