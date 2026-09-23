package com.mb.infrastructure.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link RequestLoggingFilter}.
 *
 * <p>Verifies MDC lifecycle, request-ID propagation (from header or generated), response header
 * injection, and MDC cleanup on normal and exceptional filter chain execution.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter – MDC correlation ID lifecycle")
class RequestLoggingFilterTest {

  @Mock private FilterChain filterChain;

  private RequestLoggingFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter = new RequestLoggingFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    MDC.clear();
  }

  // -------------------------------------------------------------------------
  // Request ID from header
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("uses X-Request-Id header value as MDC requestId when header is a valid UUID")
  void doFilterInternalWhenHeaderPresentUsesThatIdInMdc() throws Exception {
    String incomingId = "550e8400-e29b-41d4-a716-446655440000";
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, incomingId);
    AtomicReference<String> capturedId = new AtomicReference<>();

    filter.doFilterInternal(
        request,
        response,
        (_, _) -> capturedId.set(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)));

    assertThat(capturedId.get()).isEqualTo(incomingId);
  }

  @Test
  @DisplayName("echoes a valid UUID X-Request-Id header value back in the response")
  void doFilterInternalWhenHeaderPresentEchoesIdInResponse() throws Exception {
    String incomingId = "550e8400-e29b-41d4-a716-446655440000";
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, incomingId);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo(incomingId);
  }

  @Test
  @DisplayName("generates a new UUID when X-Request-Id header is not a valid UUID format")
  void doFilterInternalWhenHeaderIsNotUuidGeneratesNewId() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "not-a-uuid");

    filter.doFilterInternal(request, response, filterChain);

    String responseId = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
    assertThat(responseId).isNotBlank().isNotEqualTo("not-a-uuid");
  }

  // -------------------------------------------------------------------------
  // Generated request ID (no header)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("generates a non-blank UUID requestId when X-Request-Id header is absent")
  void doFilterInternalWhenNoHeaderGeneratesUuidInMdc() throws Exception {
    AtomicReference<String> capturedId = new AtomicReference<>();

    filter.doFilterInternal(
        request,
        response,
        (_, _) -> capturedId.set(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)));

    assertThat(capturedId.get()).isNotBlank();
  }

  @Test
  @DisplayName("sets generated request ID in response header when X-Request-Id is absent")
  void doFilterInternalWhenNoHeaderSetsGeneratedIdInResponse() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
  }

  // -------------------------------------------------------------------------
  // MDC cleanup
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("clears requestId from MDC after filter chain completes normally")
  void doFilterInternalClearsMdcAfterNormalCompletion() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "cleanup-test");

    filter.doFilterInternal(request, response, filterChain);

    assertThat(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
  }

  @Test
  @DisplayName("clears requestId from MDC even when filter chain throws")
  void doFilterInternalClearsMdcEvenWhenFilterChainThrows() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "error-test");
    doThrow(new ServletException("downstream failure"))
        .when(filterChain)
        .doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
  }

  @Test
  @DisplayName("clears requestId from MDC even when filter chain throws IOException")
  void doFilterInternalClearsMdcWhenFilterChainThrowsIoException() throws Exception {
    doThrow(new IOException("I/O failure")).when(filterChain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(IOException.class);

    assertThat(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
  }

  // -------------------------------------------------------------------------
  // Filter chain delegation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("delegates to the next filter in the chain")
  void doFilterInternalDelegatesToFilterChain() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  // -------------------------------------------------------------------------
  // CRLF injection sanitization (CWE-113)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("rejects CRLF-injected X-Request-Id and generates a safe UUID (CWE-113)")
  void doFilterInternalRejectsCrlfInjectionInRequestId() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "abc\r\nX-Injected: evil");

    filter.doFilterInternal(request, response, filterChain);

    String responseId = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
    assertThat(responseId)
        .doesNotContain("\r", "\n", "X-Injected", "evil")
        .isNotEqualTo("abcX-Injected: evil");
  }

  @Test
  @DisplayName("generates a UUID when X-Request-Id is only CRLF characters after stripping")
  void doFilterInternalGeneratesUuidWhenHeaderIsOnlyCrlf() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "\r\n");

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
    assertThat(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)).isNull();
  }

  // -------------------------------------------------------------------------
  // Blank header fallback
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("generates a UUID when X-Request-Id header is blank")
  void doFilterInternalWhenBlankHeaderGeneratesUuid() throws Exception {
    request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "   ");
    AtomicReference<String> capturedId = new AtomicReference<>();

    filter.doFilterInternal(
        request,
        response,
        (_, _) -> capturedId.set(MDC.get(RequestLoggingFilter.MDC_REQUEST_ID_KEY)));

    assertThat(capturedId.get()).isNotBlank().isNotEqualTo("   ");
  }
}
