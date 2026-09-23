package com.mb.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>Verifies that the filter delegates to the next element in the filter chain without modifying
 * the request or response. The JWT validation logic is a placeholder, so only pass-through
 * behaviour is asserted here.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter – pass-through filter (placeholder implementation)")
class JwtAuthenticationFilterTest {

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  // -------------------------------------------------------------------------
  // Filter chain delegation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("invokes filterChain.doFilter with the original request and response")
  void doFilterInternalDelegatesToFilterChain() throws Exception {
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  // -------------------------------------------------------------------------
  // Response integrity
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("does not modify the HTTP response status (remains 200 default)")
  void doFilterInternalDoesNotModifyResponseStatus() throws Exception {
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("does not write any content to the response body")
  void doFilterInternalDoesNotWriteResponseBody() throws Exception {
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    assertThat(response.getContentAsString()).isEmpty();
  }
}
