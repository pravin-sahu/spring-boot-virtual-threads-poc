package com.mb.modules.user.unit.controller;

import static com.mb.modules.user.testdata.UserTestDataBuilder.EMAIL;
import static com.mb.modules.user.testdata.UserTestDataBuilder.FIRST_NAME;
import static com.mb.modules.user.testdata.UserTestDataBuilder.VALID_UUID;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUserResponseDto;
import static org.mockito.Mockito.when;

import com.mb.base.MockMvcSecurityConfig;
import com.mb.common.exception.AppException;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.infrastructure.security.config.CustomAuthenticationEntryPoint;
import com.mb.infrastructure.security.config.SecurityConfig;
import com.mb.modules.user.controller.UserController;
import com.mb.modules.user.dto.response.UserResponseDto;
import com.mb.modules.user.service.UserService;
import com.mb.modules.user.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Web-layer unit tests for {@link UserController}.
 *
 * <p>Uses {@code @WebMvcTest} which loads only the web slice (controller, filters, security) and
 * applies Spring Security correctly so that {@code @WithMockUser} propagates through the filter
 * chain. The real {@link ApiResponseBuilder} and {@link
 * com.mb.common.exception.GlobalExceptionHandler} are included so that the full request / response
 * / error-handling pipeline is exercised. {@link UserService} is mocked via {@code @MockitoBean}.
 *
 * <p>{@code @WithMockUser} is applied per-method on tests that require an authenticated principal,
 * satisfying {@code anyRequest().authenticated()} in {@code SecurityConfig}. The security test
 * verifies that requests without any security context are rejected with 401. When
 * {@code @PreAuthorize("hasRole('X')")} is added to a controller method, update the annotation to
 * {@code @WithMockUser(roles = "X")} on the relevant tests and add a {@code @WithMockUser(roles =
 * "OTHER")} variant to verify that wrong-role access returns 403.
 *
 * @author rohit.kavthekar
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureRestTestClient
@Import({
  ApiResponseBuilder.class,
  MockMvcSecurityConfig.class,
  SecurityConfig.class,
  CustomAuthenticationEntryPoint.class
})
@DisplayName("UserController – GET /v1/users/{userUuid} web-layer")
class UserControllerTest {

  private static final String INVALID_UUID = "not-a-uuid";

  @Autowired private RestTestClient restClient;

  @MockitoBean private UserService userService;

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – happy path
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  @DisplayName("existing user UUID → 200 with user data in response body")
  void userByUuidWithExistingUserReturnsOk() {
    UserResponseDto dto = buildUserResponseDto();
    when(userService.userByUuid(VALID_UUID)).thenReturn(dto);

    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(true)
        .jsonPath("$.data.uuid")
        .isEqualTo(VALID_UUID.toString())
        .jsonPath("$.data.firstName")
        .isEqualTo(FIRST_NAME)
        .jsonPath("$.data.email")
        .isEqualTo(EMAIL);
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – error handling
  // -------------------------------------------------------------------------

  @Test
  @WithMockUser
  @DisplayName("user not found → 404 with success=false and error message")
  void userByUuidWhenNotFoundReturnsNotFound() {
    when(userService.userByUuid(VALID_UUID))
        .thenThrow(
            new AppException("User not found with uuid: " + VALID_UUID, HttpStatus.NOT_FOUND));

    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false)
        .jsonPath("$.message")
        .isEqualTo("User not found with uuid: " + VALID_UUID);
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
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, INVALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(false);
  }

  // -------------------------------------------------------------------------
  // GET /v1/users/{userUuid} – security
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("unauthenticated request → 401 Unauthorized")
  void userByUuidWithoutAuthenticationReturnsUnauthorized() {
    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  @WithMockUser(roles = {"USER"})
  @DisplayName(
      "authenticated USER role → 200 (endpoint requires authenticated, not a specific role)")
  void userByUuidWithUserRoleReturnsOk() {
    performUserByUuidReturnsOk();
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("authenticated ADMIN role → 200 (ADMIN can access user endpoints)")
  void userByUuidWithAdminRoleReturnsOk() {
    performUserByUuidReturnsOk();
  }

  // Extracted helper to remove test duplication flagged by SonarLint
  private void performUserByUuidReturnsOk() {
    UserResponseDto dto = buildUserResponseDto();
    when(userService.userByUuid(VALID_UUID)).thenReturn(dto);

    restClient
        .get()
        .uri(UserTestDataBuilder.USERS_BY_UUID_URL, VALID_UUID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.success")
        .isEqualTo(true);
  }
}
