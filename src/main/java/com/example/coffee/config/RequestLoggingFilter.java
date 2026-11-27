package com.example.coffee.config;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);
    long startedAtNanos = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      // No body, authorization header, request key, customer name or query-string logging.
      log.info(
          "event=http_request requestId={} method={} status={} durationMs={}",
          requestId,
          request.getMethod(),
          response.getStatus(),
          (System.nanoTime() - startedAtNanos) / 1_000_000);
      MDC.remove("requestId");
    }
  }
}
