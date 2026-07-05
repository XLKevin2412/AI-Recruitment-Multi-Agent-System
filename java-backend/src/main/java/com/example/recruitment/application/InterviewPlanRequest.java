package com.example.recruitment.application;

import java.util.ArrayList;
import java.util.List;

public class InterviewPlanRequest {

    private String interviewType = "TECHNICAL";
    private String preferredTimezone = "Asia/Shanghai";
    private List<TimeSlot> candidateAvailableSlots = new ArrayList<>();

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getPreferredTimezone() {
        return preferredTimezone;
    }

    public void setPreferredTimezone(String preferredTimezone) {
        this.preferredTimezone = preferredTimezone;
    }

    public List<TimeSlot> getCandidateAvailableSlots() {
        return candidateAvailableSlots;
    }

    public void setCandidateAvailableSlots(List<TimeSlot> candidateAvailableSlots) {
        this.candidateAvailableSlots = candidateAvailableSlots;
    }

    public static class TimeSlot {
        private String startTime;
        private String endTime;

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
