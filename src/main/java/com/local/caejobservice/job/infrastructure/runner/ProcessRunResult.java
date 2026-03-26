package com.local.caejobservice.job.infrastructure.runner;

public record ProcessRunResult(int returnCode, String workspace, String logFile, String logText) {}
