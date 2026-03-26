package com.local.caejobservice.job.infrastructure.runner;

import com.local.caejobservice.adapter.domain.CaeAdapter;
import com.local.caejobservice.adapter.infrastructure.JavaCommandUtils;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import com.local.caejobservice.common.util.TimeUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

  public ProcessRunResult run(JobEntity job, CaeAdapter adapter, Consumer<Process> onStarted) {
    try {
      String workspace = adapter.prepareWorkspace(job);
      Path workspacePath = Path.of(workspace);
      Path logFile = workspacePath.resolve("run.log");
      List<String> command = adapter.buildCommand(job);
      Files.createDirectories(workspacePath);
      Files.writeString(
          logFile,
          "# started_at="
              + TimeUtils.localNowIso()
              + System.lineSeparator()
              + "# cwd="
              + workspacePath
              + System.lineSeparator()
              + "$ "
              + formatCommand(command)
              + System.lineSeparator(),
          StandardCharsets.UTF_8);

      ProcessBuilder builder =
          new ProcessBuilder(command)
              .directory(workspacePath.toFile())
              .redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
      Process process = builder.start();
      LOGGER.info(
          "Job {} spawned process pid={} cwd={}", job.jobId(), process.pid(), workspacePath);
      onStarted.accept(process);
      int returnCode = process.waitFor();
      String logText = Files.readString(logFile, StandardCharsets.UTF_8);
      return new ProcessRunResult(
          returnCode, workspacePath.toString(), logFile.toString(), logText);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to run process.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Process execution interrupted.", exception);
    }
  }

  public void terminateProcess(Process process) {
    if (!process.isAlive()) {
      return;
    }
    LOGGER.warn("Terminating process pid={}", process.pid());
    process.destroy();
    try {
      if (!process.waitFor(5, TimeUnit.SECONDS)) {
        LOGGER.warn("Killing process pid={} after terminate timeout", process.pid());
        process.destroyForcibly();
        process.waitFor(5, TimeUnit.SECONDS);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while terminating process.", exception);
    }
  }

  static String formatCommand(List<String> command) {
    return command.stream()
        .map(ProcessRunner::quoteArg)
        .reduce((left, right) -> left + " " + right)
        .orElse("");
  }

  private static String quoteArg(String value) {
    if (value.indexOf(' ') < 0 && value.indexOf('\t') < 0 && value.indexOf('"') < 0) {
      return value;
    }
    if (JavaCommandUtils.isWindows()) {
      return "\"" + value.replace("\"", "\\\"") + "\"";
    }
    return "'" + value.replace("'", "'\\''") + "'";
  }
}
