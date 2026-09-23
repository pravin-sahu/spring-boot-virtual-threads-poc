package com.mb.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that sets a request-scoped correlation ID in SLF4J MDC before the request is
 * processed, and clears it in the finally block. Reads the ID from the {@code X-Request-Id} header
 * when present, otherwise generates a UUID. All log lines emitted during the request automatically
 * carry the ID when the log pattern includes {@code %X{requestId}}.
 *
 * @author rohit.kavthekar
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  static final String REQUEST_ID_HEADER = "X-Request-Id";
  static final String MDC_REQUEST_ID_KEY = "requestId";

  private static String resolveRequestId(String header) {
    if (header == null || header.isBlank()) return UUID.randomUUID().toString();
    try {
      return UUID.fromString(header).toString();
    } catch (IllegalArgumentException _) {
      return UUID.randomUUID().toString();
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));

    MDC.put(MDC_REQUEST_ID_KEY, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID_KEY);
    }
  }
}
