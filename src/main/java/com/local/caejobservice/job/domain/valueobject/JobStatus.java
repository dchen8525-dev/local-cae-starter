package com.local.caejobservice.job.domain.valueobject;

public enum JobStatus {
  PENDING("pending"),
  RUNNING("running"),
  SUCCESS("success"),
  FAILED("failed"),
  CANCELLED("cancelled");

  private final String value;

  JobStatus(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static JobStatus fromValue(String value) {
    for (JobStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unsupported status '" + value + "'.");
  }
}
