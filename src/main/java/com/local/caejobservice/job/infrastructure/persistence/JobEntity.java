package com.local.caejobservice.job.infrastructure.persistence;

import com.local.caejobservice.job.domain.valueobject.JobStatus;
import java.util.Map;

public record JobEntity(
    String jobId,
    String jobName,
    String tool,
    JobStatus status,
    Map<String, Object> params,
    String workspace,
    String logFile,
    Integer pid,
    Integer returnCode,
    String errorMessage,
    String createdAt,
    String startedAt,
    String finishedAt) {}
