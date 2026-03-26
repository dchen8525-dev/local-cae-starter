package com.local.caejobservice.common.exception;

public record ApiErrorResponse(
    String timestamp, int status, String error, String path, String detail) {}
