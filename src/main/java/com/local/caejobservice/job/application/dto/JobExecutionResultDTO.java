package com.local.caejobservice.job.application.dto;

import com.local.caejobservice.job.domain.valueobject.JobStatus;

public record JobExecutionResultDTO(JobStatus status, String errorMessage, String summary) {}
