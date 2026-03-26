package com.local.caejobservice.adapter.infrastructure;

import com.local.caejobservice.adapter.domain.CaeAdapter;
import com.local.caejobservice.common.config.AppProperties;
import com.local.caejobservice.job.application.dto.JobExecutionResultDTO;
import com.local.caejobservice.job.domain.valueobject.JobStatus;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnsaAdapter implements CaeAdapter {

  private final AppProperties properties;

  public AnsaAdapter(AppProperties properties) {
    this.properties = properties;
  }

  @Override
  public String toolName() {
    return "ansa";
  }

  @Override
  public void validateParams(Map<String, Object> params) {
    resolveExecutable();
    String scriptFile = requiredScriptFile(params);
    if (!Files.isRegularFile(Path.of(scriptFile))) {
      throw new IllegalArgumentException("ANSA script file does not exist: " + scriptFile);
    }
    String inputFile = readString(params, "input_file");
    if (inputFile != null && !Files.exists(Path.of(inputFile))) {
      throw new IllegalArgumentException("input_file does not exist: " + inputFile);
    }
    readStringList(params, "script_args");
    readStringList(params, "extra_args");
    readBoolean(params, "no_gui", true);
  }

  @Override
  public String prepareWorkspace(JobEntity job) {
    try {
      Path workspace = properties.getWorkspaceRoot().resolve(job.jobId());
      Files.createDirectories(workspace);
      return workspace.toString();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to create workspace.", exception);
    }
  }

  @Override
  public List<String> buildCommand(JobEntity job) {
    String executable = resolveExecutable();
    String scriptFile = requiredScriptFile(job.params());
    String inputFile = readString(job.params(), "input_file");
    List<String> scriptArgs = readStringList(job.params(), "script_args");
    List<String> extraArgs = readStringList(job.params(), "extra_args");
    boolean noGui = readBoolean(job.params(), "no_gui", true);

    List<String> command = new ArrayList<>();
    command.add(executable);
    if (noGui) {
      command.addAll(properties.getAnsaBatchFlags());
    }

    List<String> execpyParts = new ArrayList<>();
    execpyParts.add(properties.getAnsaExecpyPrefix().stripTrailing() + quoteExecpyArg(scriptFile));
    if (inputFile != null) {
      execpyParts.add(quoteExecpyArg(inputFile));
    }
    scriptArgs.stream().map(AnsaAdapter::quoteExecpyArg).forEach(execpyParts::add);

    command.add("-execpy");
    command.add(String.join(" ", execpyParts));
    command.addAll(extraArgs);
    return command;
  }

  @Override
  public JobExecutionResultDTO parseResult(JobEntity job, int returnCode, String logText) {
    if (returnCode == 0) {
      return new JobExecutionResultDTO(
          JobStatus.SUCCESS, null, "ANSA completed for job " + job.jobId() + ".");
    }
    String summary = logText.length() <= 500 ? logText : logText.substring(logText.length() - 500);
    return new JobExecutionResultDTO(
        JobStatus.FAILED, "ANSA returned a non-zero exit code.", summary);
  }

  public String resolveExecutable() {
    if (properties.getAnsaExecutable() != null) {
      Path path = Path.of(properties.getAnsaExecutable());
      if (Files.exists(path)) {
        return path.toString();
      }
      throw new IllegalArgumentException(
          "ANSA executable not found: " + properties.getAnsaExecutable());
    }

    for (String candidate : properties.getAnsaCandidatePaths()) {
      Path path = Path.of(candidate);
      if (Files.exists(path)) {
        return path.toString();
      }
    }

    throw new IllegalArgumentException(
        "ANSA executable is not configured. Set app.ansa-executable in application.yml to your ANSA"
            + " launcher, for example ansa64.bat or ansa64.exe.");
  }

  public static String quoteExecpyArg(String value) {
    if (value.indexOf(' ') < 0 && value.indexOf('\t') < 0 && value.indexOf('"') < 0) {
      return value;
    }
    return "\"" + value.replace("\"", "\\\"") + "\"";
  }

  private String requiredScriptFile(Map<String, Object> params) {
    String explicit = readString(params, "script_file");
    String resolved = explicit != null ? explicit : properties.getAnsaScriptFile();
    if (resolved == null) {
      throw new IllegalArgumentException(
          "ANSA script file is not configured. Set app.ansa-script-file in application.yml.");
    }
    return resolved;
  }

  private static String readString(Map<String, Object> params, String key) {
    Object value = params.get(key);
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? null : text;
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

  private static List<String> readStringList(Map<String, Object> params, String key) {
    Object value = params.get(key);
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> items) {
      return items.stream().map(Object::toString).toList();
    }
    throw new IllegalArgumentException("'" + key + "' must be an array of strings.");
  }
}
