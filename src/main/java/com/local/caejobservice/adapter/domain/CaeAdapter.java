package com.local.caejobservice.adapter.domain;

import com.local.caejobservice.job.application.dto.JobExecutionResultDTO;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import java.util.List;
import java.util.Map;

public interface CaeAdapter {

  String toolName();

  void validateParams(Map<String, Object> params);

  String prepareWorkspace(JobEntity job);

  List<String> buildCommand(JobEntity job);

  JobExecutionResultDTO parseResult(JobEntity job, int returnCode, String logText);
}
