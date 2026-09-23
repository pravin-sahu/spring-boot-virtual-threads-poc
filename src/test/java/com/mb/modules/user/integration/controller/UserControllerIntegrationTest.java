package com.mb.modules.user.integration.controller;

import static com.mb.modules.user.testdata.UserTestDataBuilder.EMAIL;
import static com.mb.modules.user.testdata.UserTestDataBuilder.VALID_UUID;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUser;

import com.mb.base.AbstractBaseIntegrationTest;
import com.mb.modules.user.entity.User;
import com.mb.modules.user.repository.UserRepository;
import com.mb.modules.user.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Full-stack integration tests for {@link com.mb.modules.user.controller.UserController}.
 *
 * <p>Extends {@link AbstractBaseIntegrationTest} to get a real PostgreSQL database via
 * Testcontainers. {@code @AutoConfigureRestTestClient} wires a {@link RestTestClient} against the
 * full Spring context without an embedded HTTP server.
 *
 * <p>Security is verified at the unit level in {@code UserControllerTest} using {@code @WebMvcTest}
 * which applies the real security filter chain. Integration tests here focus on verifying that the
 * full controller → service → DAO → database stack works correctly end-to-end.
 * {@code @WithMockUser} is applied per-method to satisfy {@code anyRequest().authenticated()} in
 * {@code SecurityConfig}.
 *
 * @author rohit.kavthekar
 */
@AutoConfigureRestTestClient
@DisplayName("UserController integration – GET /v1/users/{userUuid} full-stack")
class UserControllerIntegrationTest extends AbstractBaseIntegrationTest {

  @Autowired private RestTestClient restClient;
  @Autowired private UserRepository userRepo;

  @AfterEach
  void tearDown() {
    userRepo.deleteAll();
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – happy path
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  @DisplayName("existing user in database → 200 with uuid and email in response body")
  void userByUuidWithExistingUserReturnsOk() {
    User saved = userRepo.save(buildUser(EMAIL));
    String uuid = saved.getUuid().toString();

    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, uuid)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(true)
        .jsonPath("$.data.uuid")
        .isEqualTo(uuid)
        .jsonPath("$.data.email")
        .isEqualTo(EMAIL);
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – error handling
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  @DisplayName("UUID not in database → 404 with success=false")
  void userByUuidWhenNotFoundReturnsNotFound() {
    // VALID_UUID is never inserted, so this must return 404
    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false);
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – validation
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  @DisplayName("malformed UUID path variable → 400 with success=false")
  void userByUuidWithInvalidUuidReturnsBadRequest() {
    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, "not-a-uuid")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false);
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – security (full-stack filter chain)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("unauthenticated request → 401 with success=false and UNAUTHORIZED error code")
  void userByUuidWithoutAuthenticationReturnsUnauthorized() {
    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
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
}
