package com.local.caejobservice.job.api.response;

import java.util.Map;

public record JobDetailResponse(
    String jobId,
    String jobName,
    String tool,
    String status,
    Integer pid,
    Integer returnCode,
    String errorMessage,
    String workspace,
    String logFile,
    String createdAt,
    String startedAt,
    String finishedAt,
    Map<String, Object> params) {}
