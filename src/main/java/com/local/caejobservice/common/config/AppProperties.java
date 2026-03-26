package com.local.caejobservice.common.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private String name = "Local CAE Job Service";
  private Path databasePath = Path.of("data/jobs.db");
  private Path workspaceRoot = Path.of("workspaces");
  private String ansaExecutable;
  private String ansaScriptFile;
  private String ansaExecpyPrefix = "load_script:";
  private List<String> ansaBatchFlags = new ArrayList<>(List.of("-b"));
  private List<String> ansaCandidatePaths = new ArrayList<>();
  private List<String> allowedOrigins =
      new ArrayList<>(List.of("http://127.0.0.1:3000", "http://localhost:3000"));
  private double logPollIntervalSeconds = 0.5d;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Path getDatabasePath() {
    return databasePath;
  }

  public void setDatabasePath(Path databasePath) {
    this.databasePath = databasePath;
  }

  public Path getWorkspaceRoot() {
    return workspaceRoot;
  }

  public void setWorkspaceRoot(Path workspaceRoot) {
    this.workspaceRoot = workspaceRoot;
  }

  public String getAnsaExecutable() {
    return blankToNull(ansaExecutable);
  }

  public void setAnsaExecutable(String ansaExecutable) {
    this.ansaExecutable = ansaExecutable;
  }

  public String getAnsaScriptFile() {
    return blankToNull(ansaScriptFile);
  }

  public void setAnsaScriptFile(String ansaScriptFile) {
    this.ansaScriptFile = ansaScriptFile;
  }

  public String getAnsaExecpyPrefix() {
    return ansaExecpyPrefix;
  }

  public void setAnsaExecpyPrefix(String ansaExecpyPrefix) {
    this.ansaExecpyPrefix = ansaExecpyPrefix;
  }

  public List<String> getAnsaBatchFlags() {
    return ansaBatchFlags;
  }

  public void setAnsaBatchFlags(List<String> ansaBatchFlags) {
    this.ansaBatchFlags = ansaBatchFlags;
  }

  public void setAnsaBatchFlags(String ansaBatchFlags) {
    this.ansaBatchFlags = splitCommaSeparated(ansaBatchFlags, "-b");
  }

  public List<String> getAnsaCandidatePaths() {
    return ansaCandidatePaths;
  }

  public void setAnsaCandidatePaths(List<String> ansaCandidatePaths) {
    this.ansaCandidatePaths = ansaCandidatePaths;
  }

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins =
        splitCommaSeparated(allowedOrigins, "http://127.0.0.1:3000", "http://localhost:3000");
  }

  public double getLogPollIntervalSeconds() {
    return logPollIntervalSeconds;
  }

  public void setLogPollIntervalSeconds(double logPollIntervalSeconds) {
    this.logPollIntervalSeconds = logPollIntervalSeconds;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  private static List<String> splitCommaSeparated(String raw, String... defaults) {
    if (raw == null || raw.isBlank()) {
      return new ArrayList<>(List.of(defaults));
    }
    return raw.lines()
        .flatMap(line -> List.of(line.split(",")).stream())
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }
}
