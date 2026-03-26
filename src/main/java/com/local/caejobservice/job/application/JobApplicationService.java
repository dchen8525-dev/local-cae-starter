package com.local.caejobservice.job.application;

import com.local.caejobservice.adapter.domain.CaeAdapter;
import com.local.caejobservice.adapter.infrastructure.AdapterRegistry;
import com.local.caejobservice.common.config.AppProperties;
import com.local.caejobservice.job.api.request.JobCreateRequest;
import com.local.caejobservice.job.api.response.JobActionResponse;
import com.local.caejobservice.job.application.dto.JobExecutionResultDTO;
import com.local.caejobservice.job.domain.repository.JobRepository;
import com.local.caejobservice.job.domain.valueobject.JobStatus;
import com.local.caejobservice.job.infrastructure.persistence.JobEntity;
import com.local.caejobservice.job.infrastructure.runner.ProcessRunResult;
import com.local.caejobservice.job.infrastructure.runner.ProcessRunner;
import com.local.caejobservice.common.util.TimeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobApplicationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobApplicationService.class);

  private final AdapterRegistry adapterRegistry;
  private final JobRepository jobRepository;
  private final ProcessRunner processRunner;
  private final AppProperties properties;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Set<CompletableFuture<?>> tasks = ConcurrentHashMap.newKeySet();
  private final Map<String, Process> processes = new ConcurrentHashMap<>();
  private final ReentrantLock cancelLock = new ReentrantLock();

  public JobApplicationService(
      AdapterRegistry adapterRegistry,
      JobRepository jobRepository,
      ProcessRunner processRunner,
      AppProperties properties) {
    this.adapterRegistry = adapterRegistry;
    this.jobRepository = jobRepository;
    this.processRunner = processRunner;
    this.properties = properties;
  }

  @PostConstruct
  public void startup() {
    try {
      Files.createDirectories(properties.getWorkspaceRoot());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to initialize workspace root.", exception);
    }
    jobRepository.initDatabase(properties.getDatabasePath().toString());
    int affected = jobRepository.markIncompleteJobsFailed();
    if (affected > 0) {
      LOGGER.warn("Marked {} incomplete jobs as failed after restart", affected);
    }
  }

  public JobActionResponse createJob(JobCreateRequest request) {
    CaeAdapter adapter =
        adapterRegistry
            .get(request.getTool())
            .orElseThrow(
                () ->
                    badRequest(
                        "Unknown tool '"
                            + request.getTool()
                            + "'. Supported tools: "
                            + adapterRegistry.supportedTools()));

    try {
      adapter.validateParams(request.getParams());
    } catch (IllegalArgumentException exception) {
      throw badRequest("Invalid params: " + exception.getMessage());
    }

    String jobId = TimeUtils.newJobId();
    JobEntity record =
        new JobEntity(
            jobId,
            request.getJobName(),
            request.getTool(),
            JobStatus.PENDING,
            request.getParams(),
            null,
            null,
            null,
            null,
            null,
            TimeUtils.localNowIso(),
            null,
            null);
    jobRepository.insertJob(record);
    schedule(jobId);
    return new JobActionResponse(
        jobId, JobStatus.PENDING.value(), "Job accepted and scheduled.");
  }

  public JobEntity getJob(String jobId) {
    return jobRepository.getJob(jobId).orElseThrow(() -> notFound("Job '" + jobId + "' not found."));
  }

  public List<JobEntity> listJobs(String status) {
    if (status != null && !status.isBlank()) {
      JobStatus.fromValue(status);
    }
    return jobRepository.listJobs(status);
  }

  public JobActionResponse cancelJob(String jobId) {
    cancelLock.lock();
    try {
      JobEntity job = getJob(jobId);
      if (job.status() == JobStatus.CANCELLED) {
        return new JobActionResponse(jobId, job.status().value(), "Job already cancelled.");
      }
      if (job.status() == JobStatus.SUCCESS || job.status() == JobStatus.FAILED) {
        return new JobActionResponse(
            jobId, job.status().value(), "Job already finished and cannot be cancelled.");
      }

      Process process = processes.get(jobId);
      if (process == null) {
        jobRepository.markCancelled(
            jobId, "Job cancelled before process start.", TimeUtils.localNowIso(), null);
        return new JobActionResponse(jobId, JobStatus.CANCELLED.value(), "Job cancelled.");
      }

      processRunner.terminateProcess(process);
      Integer returnCode = process.isAlive() ? null : process.exitValue();
      jobRepository.markCancelled(
          jobId, "Job cancelled by user.", TimeUtils.localNowIso(), returnCode);
      return new JobActionResponse(
          jobId, JobStatus.CANCELLED.value(), "Cancellation requested.");
    } finally {
      cancelLock.unlock();
    }
  }

  public Path getLogPath(String jobId) {
    JobEntity job = getJob(jobId);
    if (job.logFile() == null || job.logFile().isBlank()) {
      throw notFound("Job '" + jobId + "' has no log file yet.");
    }
    return Path.of(job.logFile());
  }

  public Map<String, Object> healthPayload() {
    return Map.of(
        "message", "Local CAE Job Service is running.",
        "frontend", "/frontend/index.html");
  }

  @PreDestroy
  public void shutdown() {
    for (CompletableFuture<?> task : tasks) {
      task.cancel(true);
    }
    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private void schedule(String jobId) {
    CompletableFuture<Void> task = CompletableFuture.runAsync(() -> runJob(jobId), executor);
    tasks.add(task);
    task.whenComplete((ignored, throwable) -> tasks.remove(task));
  }

  private void runJob(String jobId) {
    Optional<JobEntity> maybeJob = jobRepository.getJob(jobId);
    if (maybeJob.isEmpty() || maybeJob.get().status() != JobStatus.PENDING) {
      return;
    }
    JobEntity job = maybeJob.get();
    CaeAdapter adapter =
        adapterRegistry
            .get(job.tool())
            .orElseGet(
                () -> {
                  jobRepository.markFailed(
                      jobId,
                      "No adapter registered for tool '" + job.tool() + "'.",
                      TimeUtils.localNowIso());
                  return null;
                });
    if (adapter == null) {
      return;
    }

    String startedAt = TimeUtils.localNowIso();
    try {
      ProcessRunResult result =
          processRunner.run(
              job,
              adapter,
              process -> {
                JobEntity latest = jobRepository.getJob(jobId).orElse(null);
                if (latest != null && latest.status() == JobStatus.CANCELLED) {
                  processRunner.terminateProcess(process);
                  return;
                }
                processes.put(jobId, process);
                jobRepository.markRunning(
                    jobId,
                    Path.of(properties.getWorkspaceRoot().toString(), jobId).toString(),
                    Path.of(properties.getWorkspaceRoot().toString(), jobId, "run.log").toString(),
                    Math.toIntExact(process.pid()),
                    startedAt);
              });

      JobEntity latest = jobRepository.getJob(jobId).orElse(null);
      if (latest == null) {
        return;
      }
      if (latest.status() == JobStatus.CANCELLED) {
        jobRepository.updateExecutionArtifacts(
            jobId, result.workspace(), result.logFile(), result.returnCode());
        return;
      }

      JobExecutionResultDTO parsed =
          adapter.parseResult(latest, result.returnCode(), result.logText());
      jobRepository.markFinished(
          jobId,
          parsed.status(),
          result.workspace(),
          result.logFile(),
          result.returnCode(),
          TimeUtils.localNowIso(),
          parsed.errorMessage());
    } catch (Exception exception) {
      jobRepository.markFailed(jobId, exception.getMessage(), TimeUtils.localNowIso());
    } finally {
      processes.remove(jobId);
    }
  }

  private static ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}
