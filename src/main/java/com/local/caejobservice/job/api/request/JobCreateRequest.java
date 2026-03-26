package com.local.caejobservice.job.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobCreateRequest {

  @NotBlank
  @Size(max = 200)
  private String jobName;

  @NotBlank
  @Size(max = 100)
  private String tool;

  @NotNull private Map<String, Object> params = new LinkedHashMap<>();

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = normalizeRequired(jobName, "job_name must not be empty");
  }

  public String getTool() {
    return tool;
  }

  public void setTool(String tool) {
    this.tool = normalizeRequired(tool, "tool must not be empty").toLowerCase();
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
  }

  private static String normalizeRequired(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
