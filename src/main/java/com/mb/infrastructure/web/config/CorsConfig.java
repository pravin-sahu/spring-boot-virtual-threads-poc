package com.mb.infrastructure.web.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * This class configures CORS (Cross-Origin Resource Sharing) for the application. It defines a
 * CorsConfigurationSource bean that specifies the allowed origins, methods, headers, and exposed
 * headers for cross-origin requests.
 *
 * @author rohit.kavthekar
 */
@Configuration
public class CorsConfig {

  @Value("${app.cors.allowed.origins}")
  private List<String> allowedOrigins;

  private static final List<String> allowedMethods =
      List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

  private static final List<String> allowedHeaders =
      List.of(
          "Authorization", // JWT Bearer token
          "Content-Type", // standard header for request body content type
          "X-Correlation-Id", // client can optionally forward a trace ID
          "X-Tenant-Id", // multi-tenancy
          "X-Request-Id", // client can optionally forward a unique request ID for tracing
          "X-Api-Version", // API versioning (rename from "Version" — more explicit),
          "X-Idempotency-Key" // for safely retrying non-idempotent requests
          );

  private static final List<String> exposedHeaders =
      List.of(
          "Authorization", // JWT Bearer token
          "Content-Disposition", // file download filename
          "X-Correlation-Id",
          "X-Request-Id",
          "X-Tenant-Id",
          "X-Api-Version",
          "X-Idempotency-Key");

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Set to false for JWT-based API (no cookies needed)
    // If you MUST support cookies, enable CSRF protection
    configuration.setAllowCredentials(false);

    configuration.setAllowedMethods(allowedMethods);
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedHeaders(allowedHeaders);
    configuration.setExposedHeaders(exposedHeaders);

    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
