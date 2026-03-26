package com.local.caejobservice.job.application.assembler;

import com.local.caejobservice.job.api.response.JobDetailResponse;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import org.springframework.stereotype.Component;

@Component
public class JobResponseAssembler {

  public JobDetailResponse toDetailResponse(JobEntity job) {
    return new JobDetailResponse(
        job.jobId(),
        job.jobName(),
        job.tool(),
        job.status().value(),
        job.pid(),
        job.returnCode(),
        job.errorMessage(),
        job.workspace(),
        job.logFile(),
        job.createdAt(),
        job.startedAt(),
        job.finishedAt(),
        job.params());
  }
}
