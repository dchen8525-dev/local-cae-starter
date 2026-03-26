package com.local.caejobservice.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startNanos = System.nanoTime();
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      String requestBody = previewBody(wrappedRequest.getContentAsByteArray());
      double durationMs = (System.nanoTime() - startNanos) / 1_000_000.0d;
      LOGGER.info(
          "HTTP response method={} path={} query={} status_code={} duration_ms={} body={}",
          request.getMethod(),
          request.getRequestURI(),
          request.getQueryString(),
          wrappedResponse.getStatus(),
          "%.2f".formatted(durationMs),
          requestBody);
      wrappedResponse.copyBodyToResponse();
    }
  }

  private static String previewBody(byte[] body) {
    if (body.length == 0) {
      return "";
    }
    String text = new String(body, StandardCharsets.UTF_8);
    if (text.length() <= 500) {
      return text;
    }
    return "%s...(%d chars)".formatted(text.substring(0, 500), text.length());
  }
}
