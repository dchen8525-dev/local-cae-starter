package com.local.caejobservice.common.config;

import com.local.caejobservice.job.infrastructure.websocket.JobLogWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final JobLogWebSocketHandler handler;
  private final AppProperties properties;

  public WebSocketConfig(JobLogWebSocketHandler handler, AppProperties properties) {
    this.handler = handler;
    this.properties = properties;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws/jobs/{jobId}")
        .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
  }
}
