package com.local.caejobservice.adapter.infrastructure;

import com.local.caejobservice.adapter.domain.CaeAdapter;
import com.local.caejobservice.common.config.AppProperties;
import com.local.caejobservice.job.application.dto.JobExecutionResultDTO;
import com.local.caejobservice.job.domain.valueobject.JobStatus;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DummySolverAdapter implements CaeAdapter {

  private final AppProperties properties;

  public DummySolverAdapter(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public String toolName() {
    return "dummy_solver";
  }

  @Override
  public void validateParams(Map<String, Object> params) {
    int duration = readInt(params, "duration", 10);
    if (duration < 1 || duration > 3600) {
      throw new IllegalArgumentException("duration must be between 1 and 3600.");
    }
    readBoolean(params, "fail", false);
  }

  @Override
  public String prepareWorkspace(JobEntity job) {
    try {
      java.nio.file.Path workspace = properties.getWorkspaceRoot().resolve(job.jobId());
      Files.createDirectories(workspace);
      return workspace.toString();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to create workspace.", exception);
    }
  }

  @Override
  public List<String> buildCommand(JobEntity job) {
    int duration = readInt(job.params(), "duration", 10);
    boolean fail = readBoolean(job.params(), "fail", false);
    List<String> command = new ArrayList<>();
    command.add(JavaCommandUtils.javaExecutable());
    command.add("-cp");
    command.add(JavaCommandUtils.currentClasspath());
    command.add("com.local.caejobservice.adapter.infrastructure.DummySolverMain");
    command.add("--duration");
    command.add(Integer.toString(duration));
    command.add("--fail");
    command.add(Boolean.toString(fail));
    return command;
  }

  @Override
  public JobExecutionResultDTO parseResult(JobEntity job, int returnCode, String logText) {
    if (returnCode == 0) {
      return new JobExecutionResultDTO(
          JobStatus.SUCCESS, null, "Dummy solver completed for job " + job.jobId() + ".");
    }
    String summary = logText.length() <= 500 ? logText : logText.substring(logText.length() - 500);
    return new JobExecutionResultDTO(
        JobStatus.FAILED, "Dummy solver reported a non-zero exit code.", summary);
  }

  private static int readInt(Map<String, Object> params, String key, int defaultValue) {
    Object value = params.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(value.toString());
  }

  private static boolean readBoolean(Map<String, Object> params, String key, boolean defaultValue) {
    Object value = params.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean flag) {
      return flag;
    }
    return switch (value.toString().trim().toLowerCase()) {
      case "true", "1", "yes" -> true;
      case "false", "0", "no" -> false;
      default -> throw new IllegalArgumentException("Expected boolean value for '" + key + "'.");
    };
  }
}
