package com.mb.infrastructure.security.integration;

import com.mb.base.AbstractBaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Full-stack integration tests for endpoint access-control rules defined in {@code SecurityConfig}.
 *
 * <p>Covers three security scenarios per endpoint class:
 *
 * <ul>
 *   <li>Unauthenticated → 401 Unauthorized
 *   <li>Authenticated with insufficient role → 403 Forbidden
 *   <li>Authenticated with correct role → 200 OK
 * </ul>
 *
 * <p>Public endpoints ({@code /actuator/health}, {@code /actuator/info}, OPTIONS preflight) are
 * verified to be accessible without any credentials.
 *
 * @author rohit.kavthekar
 */
@AutoConfigureRestTestClient
@DisplayName("Security – endpoint access rules (unauthenticated / authenticated / forbidden)")
class SecurityIntegrationTest extends AbstractBaseIntegrationTest {

  @Autowired private RestTestClient restClient;

  // -------------------------------------------------------------------------
  // Public endpoints – no auth required (permitAll)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("GET /actuator/health → 200 without authentication (publicly exposed)")
  void actuatorHealthIsPubliclyAccessible() {
    restClient
        .get()
        .uri("/actuator/health")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @DisplayName("GET /actuator/info → 200 without authentication (publicly exposed)")
  void actuatorInfoIsPubliclyAccessible() {
    restClient
        .get()
        .uri("/actuator/info")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @DisplayName("OPTIONS /** → 200 for CORS preflight without authentication")
  void optionsPreflightIsPermittedWithoutAuthentication() {
    restClient
        .options()
        .uri("/v1/users/550e8400-e29b-41d4-a716-446655440000")
        .exchange()
        .expectStatus()
        .isOk();
  }

  // -------------------------------------------------------------------------
  // Secured endpoints – unauthenticated → 401
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("GET /actuator/metrics without auth → 401 Unauthorized with JSON error body")
  void actuatorMetricsUnauthenticatedReturnsUnauthorized() {
    restClient
        .get()
        .uri("/actuator/metrics")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.errorCode")
        .isEqualTo("UNAUTHORIZED");
  }

  @Test
  @DisplayName("GET /v1/users/{uuid} without auth → 401 Unauthorized with JSON error body")
  void userEndpointUnauthenticatedReturnsUnauthorized() {
    restClient
        .get()
        .uri("/v1/users/550e8400-e29b-41d4-a716-446655440000")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.errorCode")
        .isEqualTo("UNAUTHORIZED");
  }

  // -------------------------------------------------------------------------
  // Secured endpoints – authenticated with insufficient role → 403
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = {"USER"})
  @DisplayName("GET /actuator/metrics with USER role → 403 Forbidden (requires ADMIN role)")
  void actuatorMetricsWithUserRoleReturnsForbidden() {
    restClient
        .get()
        .uri("/actuator/metrics")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  // -------------------------------------------------------------------------
  // Secured endpoints – authenticated with correct role → 200
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("GET /actuator/metrics with ADMIN role → 200 OK")
  void actuatorMetricsWithAdminRoleReturnsOk() {
    restClient
        .get()
        .uri("/actuator/metrics")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk();
  }
}
