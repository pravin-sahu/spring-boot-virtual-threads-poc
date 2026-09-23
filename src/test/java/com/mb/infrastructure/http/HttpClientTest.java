package com.mb.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.common.exception.AppException;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

/**
 * Unit tests for {@link HttpClient}.
 *
 * <p>Verifies HTTP client wrapper functionality including request building, error handling, query
 * parameter handling, and custom header support. Uses Mockito to verify proper delegation to
 * Spring's RestClient and appropriate wrapping of exceptions in AppException.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpClient – RestClient wrapper for external HTTP calls")
class HttpClientTest {

  private static final String TEST_API_URL = "http://example.com/api/data";
  private static final String OK_RESPONSE = "ok-response";
  private static final String PAGED_RESPONSE = "paged-response";
  private static final String RESPONSE_WITH_HEADER = "response-with-header";
  private static final String CONNECTION_REFUSED_ERROR = "connection refused";

  @Mock private RestClient restClient;
  @Mock private RequestBodyUriSpec requestBodyUriSpec;
  @Mock private RequestBodySpec requestBodySpec;
  @Mock private ResponseSpec responseSpec;

  private HttpClient httpClient;

  @BeforeEach
  void setUp() {
    httpClient = new HttpClient(restClient);
  }

  // -------------------------------------------------------------------------
  // Happy-path execution
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("GET request with no extras → response body returned as-is")
  void executeWhenRestClientSucceedsReturnsResponse() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder().url(TEST_API_URL).method(HttpMethod.GET).build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(OK_RESPONSE);
  }

  // -------------------------------------------------------------------------
  // Error handling
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("RestClient throws RuntimeException → wrapped in AppException with 500 status")
  void executeWhenRestClientThrowsExceptionWrapsInAppException() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder().url(TEST_API_URL).method(HttpMethod.GET).build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenThrow(new RuntimeException(CONNECTION_REFUSED_ERROR));

    assertThatThrownBy(() -> httpClient.execute(request, String.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              assertThat(ae.getDetail()).contains(CONNECTION_REFUSED_ERROR);
            });
  }

  // -------------------------------------------------------------------------
  // Query parameters
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("request with queryParams map → params appended to URI, response returned")
  void executeWithQueryParamsAppendsParamsToUri() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .queryParams(Map.of("page", "1", "size", "10"))
            .build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(PAGED_RESPONSE);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(PAGED_RESPONSE);
  }

  // -------------------------------------------------------------------------
  // Custom headers
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("request with custom header map → header included and response returned")
  void executeWithCustomHeadersIncludesHeadersInRequest() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .headers(Map.of("X-Correlation-Id", "test-id-123"))
            .build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(RESPONSE_WITH_HEADER);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(RESPONSE_WITH_HEADER);
  }

  // -------------------------------------------------------------------------
  // Additional coverage tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("request with null headers → headers not set, response returned")
  void executeWithNullHeadersSkipsHeaderConfiguration() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder().url(TEST_API_URL).method(HttpMethod.GET).headers(null).build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(OK_RESPONSE);
  }

  @Test
  @DisplayName("request with null queryParams → no query parameters added, response returned")
  void executeWithNullQueryParamsHandledGracefully() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .queryParams(null)
            .build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(OK_RESPONSE);
  }

  @Test
  @DisplayName("request with empty queryParams map → no parameters added, response returned")
  void executeWithEmptyQueryParamsMapHandledGracefully() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .queryParams(Map.of())
            .build();

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo(OK_RESPONSE);
  }

  @Test
  @DisplayName("POST request with body → body included in request, response returned")
  void executePostRequestWithBodyIncludesBodyInRequest() {
    String requestBody = "{\"name\": \"John\", \"email\": \"john@example.com\"}";
    HttpRequest<String> request =
        HttpRequest.<String>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.POST)
            .body(requestBody)
            .headers(Map.of("Content-Type", "application/json"))
            .build();

    when(restClient.method(HttpMethod.POST)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn("{\"id\": 123}");

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo("{\"id\": 123}");
  }

  @Test
  @DisplayName("PUT request with complex query params and headers → all elements included")
  void executePutRequestWithComplexParametersIncludesAllElements() {
    String requestBody = "{\"status\": \"updated\"}";
    Map<String, String> headers =
        Map.of(
            "Content-Type", "application/json",
            "X-Request-ID", "req-123",
            "Authorization", "Bearer token456");
    Map<String, String> queryParams =
        Map.of(
            "version", "v2",
            "includeMetadata", "true",
            "format", "json");

    HttpRequest<String> request =
        HttpRequest.<String>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.PUT)
            .body(requestBody)
            .headers(headers)
            .queryParams(queryParams)
            .build();

    when(restClient.method(HttpMethod.PUT)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn("{\"updated\": true}");

    String result = httpClient.execute(request, String.class);

    assertThat(result).isEqualTo("{\"updated\": true}");
  }

  // -------------------------------------------------------------------------
  // Headers lambda function coverage tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("headers consumer lambda with non-null headers → h.setAll() called with headers")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void headersConsumerLambdaWithNonNullHeadersCallsSetAll() {
    Map<String, String> requestHeaders = Map.of("Authorization", "Bearer test-token");
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .headers(requestHeaders)
            .build();

    // Create a mock HttpHeaders to capture the setAll call
    HttpHeaders mockHttpHeaders = org.mockito.Mockito.mock(HttpHeaders.class);
    ArgumentCaptor<Consumer> headersCaptor = ArgumentCaptor.forClass(Consumer.class);

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(headersCaptor.capture())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    httpClient.execute(request, String.class);

    // Verify the headers consumer lambda was captured and execute it
    Consumer<HttpHeaders> headersConsumer = headersCaptor.getValue();
    headersConsumer.accept(mockHttpHeaders);

    // Verify that setAll was called with the correct headers
    verify(mockHttpHeaders).setAll(requestHeaders);
  }

  @Test
  @DisplayName("headers consumer lambda with null headers → h.setAll() not called")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void headersConsumerLambdaWithNullHeadersDoesNotCallSetAll() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder()
            .url(TEST_API_URL)
            .method(HttpMethod.GET)
            .headers(null) // Explicitly null headers
            .build();

    // Create a mock HttpHeaders to capture any calls
    HttpHeaders mockHttpHeaders = org.mockito.Mockito.mock(HttpHeaders.class);
    ArgumentCaptor<Consumer> headersCaptor = ArgumentCaptor.forClass(Consumer.class);

    when(restClient.method(HttpMethod.GET)).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.headers(headersCaptor.capture())).thenReturn(requestBodySpec);
    when(requestBodySpec.body((Object) any())).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(String.class)).thenReturn(OK_RESPONSE);

    httpClient.execute(request, String.class);

    // Verify the headers consumer lambda was captured and execute it
    Consumer<HttpHeaders> headersConsumer = headersCaptor.getValue();
    headersConsumer.accept(mockHttpHeaders);

    // Verify that setAll was never called since headers were null
    verify(mockHttpHeaders, org.mockito.Mockito.never()).setAll(any());
  }
}
