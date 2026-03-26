package com.local.caejobservice.job.infrastructure.websocket;

import com.local.caejobservice.common.config.AppProperties;
import com.local.caejobservice.job.application.JobApplicationService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

@Component
public class JobLogWebSocketHandler extends TextWebSocketHandler {

  private static final UriTemplate URI_TEMPLATE = new UriTemplate("/ws/jobs/{jobId}");
  private static final String STREAM_THREAD_KEY = "log-stream-thread";
  private final JobApplicationService jobApplicationService;
  private final long pollIntervalMillis;

  public JobLogWebSocketHandler(
      JobApplicationService jobApplicationService, AppProperties properties) {
    this.jobApplicationService = jobApplicationService;
    this.pollIntervalMillis =
        Math.max(100L, Math.round(properties.getLogPollIntervalSeconds() * 1000.0d));
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String jobId = extractJobId(session);
    Path logPath;
    try {
      logPath = jobApplicationService.getLogPath(jobId);
    } catch (Exception exception) {
      session.sendMessage(
          new TextMessage("{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}"));
      session.close(CloseStatus.NORMAL);
      return;
    }

    if (!Files.exists(logPath)) {
      session.sendMessage(new TextMessage("{\"error\":\"Log file does not exist yet.\"}"));
      session.close(CloseStatus.NORMAL);
      return;
    }

    Thread streamThread =
        Thread.ofVirtual().name("job-log-stream-" + jobId).start(() -> streamLog(session, logPath));
    session.getAttributes().put(STREAM_THREAD_KEY, streamThread);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stopStream(session);
    super.afterConnectionClosed(session, status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    stopStream(session);
    super.handleTransportError(session, exception);
  }

  private void streamLog(WebSocketSession session, Path logPath) {
    long position = 0L;
    try {
      while (session.isOpen() && !Thread.currentThread().isInterrupted()) {
        byte[] bytes = Files.readAllBytes(logPath);
        if (bytes.length > position) {
          String chunk =
              new String(
                  bytes, (int) position, bytes.length - (int) position, StandardCharsets.UTF_8);
          position = bytes.length;
          if (!chunk.isEmpty()) {
            session.sendMessage(new TextMessage(chunk));
          }
        }
        Thread.sleep(pollIntervalMillis);
      }
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } catch (IOException exception) {
      closeQuietly(session, CloseStatus.SERVER_ERROR);
    } catch (Exception exception) {
      closeQuietly(session, CloseStatus.SERVER_ERROR);
    }
  }

  private void stopStream(WebSocketSession session) {
    Object thread = session.getAttributes().remove(STREAM_THREAD_KEY);
    if (thread instanceof Thread worker) {
      worker.interrupt();
    }
  }

  private static String extractJobId(WebSocketSession session) {
    Map<String, String> variables = URI_TEMPLATE.match(session.getUri().getPath());
    return variables.get("jobId");
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static void closeQuietly(WebSocketSession session, CloseStatus status) {
    try {
      if (session.isOpen()) {
        session.close(status);
      }
    } catch (IOException ignored) {
    }
  }
}
