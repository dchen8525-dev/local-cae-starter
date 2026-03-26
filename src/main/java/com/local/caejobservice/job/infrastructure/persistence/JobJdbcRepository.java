package com.local.caejobservice.job.infrastructure.persistence;

import com.local.caejobservice.job.domain.valueobject.JobStatus;
import com.local.caejobservice.job.domain.repository.JobRepository;
import com.local.caejobservice.common.util.JsonUtils;
import com.local.caejobservice.common.util.TimeUtils;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JobJdbcRepository implements JobRepository {

  private static final RowMapper<JobEntity> ROW_MAPPER = JobJdbcRepository::mapRow;
  private final JdbcTemplate jdbcTemplate;

  public JobJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void initDatabase(String databasePath) {
    try {
      Files.createDirectories(java.nio.file.Path.of(databasePath).toAbsolutePath().getParent());
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to initialize database directory.", exception);
    }
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS jobs (
            id TEXT PRIMARY KEY,
            job_name TEXT NOT NULL,
            tool TEXT NOT NULL,
            status TEXT NOT NULL,
            params_json TEXT NOT NULL,
            workspace TEXT,
            log_file TEXT,
            pid INTEGER,
            return_code INTEGER,
            error_message TEXT,
            created_at TEXT NOT NULL,
            started_at TEXT,
            finished_at TEXT
        )
        """);
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status)");
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs(created_at)");
  }

  @Override
  public void insertJob(JobEntity record) {
    jdbcTemplate.update(
        """
        INSERT INTO jobs (
            id, job_name, tool, status, params_json, workspace, log_file,
            pid, return_code, error_message, created_at, started_at, finished_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.jobId(),
        record.jobName(),
        record.tool(),
        record.status().value(),
        JsonUtils.writeJson(record.params()),
        record.workspace(),
        record.logFile(),
        record.pid(),
        record.returnCode(),
        record.errorMessage(),
        record.createdAt(),
        record.startedAt(),
        record.finishedAt());
  }

  @Override
  public void markCancelled(String jobId, String errorMessage, String finishedAt, Integer returnCode) {
    LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
    updates.put("status", JobStatus.CANCELLED.value());
    updates.put("error_message", errorMessage);
    updates.put("finished_at", finishedAt);
    if (returnCode != null) {
      updates.put("return_code", returnCode);
    }
    updateColumns(jobId, updates);
  }

  @Override
  public void markFailed(String jobId, String errorMessage, String finishedAt) {
    updateColumns(
        jobId,
        Map.of(
            "status", JobStatus.FAILED.value(),
            "error_message", errorMessage,
            "finished_at", finishedAt));
  }

  @Override
  public void markRunning(
      String jobId, String workspace, String logFile, Integer pid, String startedAt) {
    updateColumns(
        jobId,
        Map.of(
            "status", JobStatus.RUNNING.value(),
            "workspace", workspace,
            "log_file", logFile,
            "pid", pid,
            "started_at", startedAt));
  }

  @Override
  public void updateExecutionArtifacts(
      String jobId, String workspace, String logFile, Integer returnCode) {
    LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
    updates.put("workspace", workspace);
    updates.put("log_file", logFile);
    updates.put("return_code", returnCode);
    updateColumns(jobId, updates);
  }

  @Override
  public void markFinished(
      String jobId,
      JobStatus status,
      String workspace,
      String logFile,
      Integer returnCode,
      String finishedAt,
      String errorMessage) {
    LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
    updates.put("status", status.value());
    updates.put("workspace", workspace);
    updates.put("log_file", logFile);
    updates.put("return_code", returnCode);
    updates.put("finished_at", finishedAt);
    if (errorMessage != null) {
      updates.put("error_message", errorMessage);
    }
    updateColumns(jobId, updates);
  }

  private void updateColumns(String jobId, Map<String, Object> updates) {
    if (updates.isEmpty()) {
      return;
    }
    List<Object> values = new ArrayList<>();
    String assignments =
        updates.entrySet().stream()
            .map(
                entry -> {
                  values.add(entry.getValue());
                  return entry.getKey() + " = ?";
                })
            .reduce((left, right) -> left + ", " + right)
            .orElseThrow();
    values.add(jobId);
    jdbcTemplate.update("UPDATE jobs SET " + assignments + " WHERE id = ?", values.toArray());
  }

  @Override
  public Optional<JobEntity> getJob(String jobId) {
    List<JobEntity> results =
        jdbcTemplate.query("SELECT * FROM jobs WHERE id = ?", ROW_MAPPER, jobId);
    return results.stream().findFirst();
  }

  @Override
  public List<JobEntity> listJobs(String status) {
    if (status == null || status.isBlank()) {
      return jdbcTemplate.query("SELECT * FROM jobs ORDER BY created_at DESC", ROW_MAPPER);
    }
    return jdbcTemplate.query(
        "SELECT * FROM jobs WHERE status = ? ORDER BY created_at DESC", ROW_MAPPER, status);
  }

  @Override
  public int markIncompleteJobsFailed() {
    return jdbcTemplate.update(
        """
        UPDATE jobs
        SET status = ?, finished_at = ?, error_message = ?
        WHERE status IN (?, ?)
        """,
        JobStatus.FAILED.value(),
        TimeUtils.localNowIso(),
        "Service restarted before job completion.",
        JobStatus.PENDING.value(),
        JobStatus.RUNNING.value());
  }

  private static JobEntity mapRow(ResultSet row, int ignored) throws SQLException {
    return new JobEntity(
        row.getString("id"),
        row.getString("job_name"),
        row.getString("tool"),
        JobStatus.fromValue(row.getString("status")),
        JsonUtils.readJsonMap(row.getString("params_json")),
        row.getString("workspace"),
        row.getString("log_file"),
        (Integer) row.getObject("pid"),
        (Integer) row.getObject("return_code"),
        row.getString("error_message"),
        row.getString("created_at"),
        row.getString("started_at"),
        row.getString("finished_at"));
  }
}
