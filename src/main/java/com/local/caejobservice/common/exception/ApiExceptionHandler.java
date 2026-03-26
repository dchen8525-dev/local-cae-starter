package com.local.caejobservice.common.exception;

import com.local.caejobservice.common.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(
      ResponseStatusException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    return build(status, request, exception.getReason());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ApiErrorResponse> handleBadRequest(
      Exception exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, request, exception.getMessage());
  }

  private static ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, HttpServletRequest request, String detail) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            TimeUtils.localNowIso(),
            status.value(),
            status.getReasonPhrase(),
            request.getRequestURI(),
            detail);
    return ResponseEntity.status(status).body(body);
  }
}
