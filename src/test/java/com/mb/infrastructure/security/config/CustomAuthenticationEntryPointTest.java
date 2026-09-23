package com.mb.infrastructure.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mb.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link CustomAuthenticationEntryPoint}.
 *
 * <p>Verifies that unauthorized requests receive an HTTP 401 response with a JSON body that matches
 * the application's standard {@link com.mb.common.dto.response.ApiResponse} structure. Uses Spring
 * MVC mock request/response objects to avoid starting a full servlet container.
 *
 * @author rohit.kavthekar
 */
@DisplayName("CustomAuthenticationEntryPoint – JSON 401 response on unauthenticated access")
class CustomAuthenticationEntryPointTest {

  private static final String AUTHENTICATION_REQUIRED_MESSAGE = "Authentication required";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final CustomAuthenticationEntryPoint entryPoint =
      new CustomAuthenticationEntryPoint(objectMapper);

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  // -------------------------------------------------------------------------
  // HTTP status and content-type
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("sets HTTP 401 Unauthorized status")
  void commenceSetsUnauthorizedStatus() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  @DisplayName("sets Content-Type to application/json")
  void commenceSetsJsonContentType() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
  }

  // -------------------------------------------------------------------------
  // Response body structure
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("response body contains success=false")
  void commenceWritesSuccessFalseInBody() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getContentAsString()).contains("\"success\":false");
  }

  @Test
  @DisplayName("response body contains 'Authentication required' message")
  void commenceWritesAuthenticationRequiredMessage() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getContentAsString()).contains(AUTHENTICATION_REQUIRED_MESSAGE);
  }

  @Test
  @DisplayName("response body contains UNAUTHORIZED error code")
  void commenceWritesUnauthorizedErrorCode() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("token expired"));

    assertThat(response.getContentAsString()).contains(ErrorCode.UNAUTHORIZED.name());
  }

  @Test
  @DisplayName("response body contains a timestamp field")
  void commenceWritesTimestampInBody() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(response.getContentAsString()).contains("timestamp");
  }

  @Test
  @DisplayName("response body is valid JSON")
  void commenceWritesValidJson() throws Exception {
    entryPoint.commence(request, response, new BadCredentialsException("bad token"));

    assertThat(objectMapper.readTree(response.getContentAsString())).isNotNull();
  }

  // -------------------------------------------------------------------------
  // Exception type independence
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("InsufficientAuthenticationException → still returns 401 with correct message")
  void commenceWithInsufficientAuthenticationExceptionStillReturnsUnauthorized() throws Exception {
    entryPoint.commence(
        request, response, new InsufficientAuthenticationException("Full auth required"));

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getContentAsString()).contains(AUTHENTICATION_REQUIRED_MESSAGE);
  }
}
