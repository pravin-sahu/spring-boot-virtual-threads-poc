package com.mb.infrastructure.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Unit tests for {@link CorsConfig}.
 *
 * <p>Verifies that the {@link CorsConfigurationSource} bean is built with the correct allowed
 * origins, methods, headers, and policy settings. The {@code allowedOrigins} property is injected
 * via {@link ReflectionTestUtils} to simulate Spring's {@code @Value} injection without loading a
 * full application context.
 *
 * @author rohit.kavthekar
 */
@DisplayName("CorsConfig – CorsConfigurationSource bean construction")
class CorsConfigTest {

  private static final String TEST_ORIGIN = "http://localhost:3000";

  private CorsConfig corsConfig;
  private CorsConfiguration configuration;

  @BeforeEach
  void setUp() {
    corsConfig = new CorsConfig();
    ReflectionTestUtils.setField(corsConfig, "allowedOrigins", List.of(TEST_ORIGIN));

    CorsConfigurationSource source = corsConfig.corsConfigurationSource();
    configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/v1/users"));
  }

  @Test
  @DisplayName("corsConfigurationSource bean is non-null")
  void corsConfigurationSourceBeanIsNonNull() {
    assertThat(corsConfig.corsConfigurationSource()).isNotNull();
  }

  @Test
  @DisplayName("configured origin is present in allowed origins")
  void allowedOriginsContainsInjectedOrigin() {
    assertThat(configuration.getAllowedOrigins()).containsExactly(TEST_ORIGIN);
  }

  @Test
  @DisplayName("standard HTTP methods are all allowed")
  void allowedMethodsContainsStandardMethods() {
    assertThat(configuration.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
  }

  @Test
  @DisplayName("Authorization and Content-Type headers are allowed")
  void allowedHeadersContainsAuthorizationAndContentType() {
    assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type");
  }

  @Test
  @DisplayName("tracing and idempotency headers are allowed")
  void allowedHeadersContainsTracingHeaders() {
    assertThat(configuration.getAllowedHeaders())
        .contains(
            "X-Correlation-Id",
            "X-Request-Id",
            "X-Tenant-Id",
            "X-Api-Version",
            "X-Idempotency-Key");
  }

  @Test
  @DisplayName("allowCredentials is false for JWT-based API")
  void allowCredentialsIsFalse() {
    assertThat(configuration.getAllowCredentials()).isFalse();
  }

  @Test
  @DisplayName("preflight cache max-age is 3600 seconds")
  void maxAgeIsOneHour() {
    assertThat(configuration.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  @DisplayName("CORS configuration applies to all paths")
  void corsConfigurationAppliesToAllPaths() {
    CorsConfigurationSource source = corsConfig.corsConfigurationSource();

    assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/"))).isNotNull();
    assertThat(source.getCorsConfiguration(new MockHttpServletRequest("POST", "/v1/users")))
        .isNotNull();
    assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/actuator/health")))
        .isNotNull();
  }
}
