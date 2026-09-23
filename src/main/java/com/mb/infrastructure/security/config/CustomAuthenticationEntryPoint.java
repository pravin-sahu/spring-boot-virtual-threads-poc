package com.mb.infrastructure.security.config;

import com.mb.common.dto.response.ApiResponse;
import com.mb.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Custom authentication entry point that returns a JSON response body for unauthorized requests
 * instead of just a status code.
 *
 * @author rohit.kavthekar
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    // Set response status and content type
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    // Create error response using the same structure as ApiResponse
    ApiResponse<Object> apiResponse =
        ApiResponse.builder()
            .success(false)
            .message("Authentication required")
            .errorCode(ErrorCode.UNAUTHORIZED)
            .timestamp(Instant.now())
            .build();

    // Write JSON response
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.getWriter().flush();
  }
}
