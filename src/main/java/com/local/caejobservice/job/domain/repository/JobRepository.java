package com.local.caejobservice.job.domain.repository;

import com.local.caejobservice.job.domain.valueobject.JobStatus;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import java.util.List;
import java.util.Optional;

public interface JobRepository {

  void initDatabase(String databasePath);

  void insertJob(JobEntity record);

  void markCancelled(String jobId, String errorMessage, String finishedAt, Integer returnCode);

  void markFailed(String jobId, String errorMessage, String finishedAt);

  void markRunning(String jobId, String workspace, String logFile, Integer pid, String startedAt);

  void updateExecutionArtifacts(String jobId, String workspace, String logFile, Integer returnCode);

  void markFinished(
      String jobId,
      JobStatus status,
      String workspace,
      String logFile,
      Integer returnCode,
      String finishedAt,
      String errorMessage);

  Optional<JobEntity> getJob(String jobId);

  List<JobEntity> listJobs(String status);

  int markIncompleteJobsFailed();
}
