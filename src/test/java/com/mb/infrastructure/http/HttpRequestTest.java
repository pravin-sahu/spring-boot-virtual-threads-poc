package com.mb.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/**
 * Unit tests for {@link HttpRequest}.
 *
 * <p>Verifies the builder pattern, getters, and constructor functionality of the HttpRequest data
 * class. These tests ensure that all properties are correctly set and retrieved, providing complete
 * coverage for the data transfer object used in HTTP client operations.
 *
 * @author rohit.kavthekar
 */
@DisplayName("HttpRequest – HTTP request data transfer object")
class HttpRequestTest {

  private static final String TEST_URL = "https://api.example.com/users";
  private static final String REQUEST_BODY =
      "{\"name\": \"John\", \"email\": \"john@example.com\"}";

  // -------------------------------------------------------------------------
  // Builder pattern tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("builder with all properties → all getters return correct values")
  void builderWithAllPropertiesCreatesCorrectRequest() {
    Map<String, String> headers = Map.of("Authorization", "Bearer token123");
    Map<String, String> queryParams = Map.of("page", "1", "limit", "10");

    HttpRequest<String> request =
        HttpRequest.<String>builder()
            .url(TEST_URL)
            .method(HttpMethod.POST)
            .body(REQUEST_BODY)
            .headers(headers)
            .queryParams(queryParams)
            .build();

    assertThat(request.getUrl()).isEqualTo(TEST_URL);
    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
    assertThat(request.getBody()).isEqualTo(REQUEST_BODY);
    assertThat(request.getHeaders()).isEqualTo(headers);
    assertThat(request.getQueryParams()).isEqualTo(queryParams);
  }

  @Test
  @DisplayName("builder with minimal properties → non-set properties are null")
  void builderWithMinimalPropertiesHasNullForOptionalFields() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder().url(TEST_URL).method(HttpMethod.GET).build();

    assertThat(request.getUrl()).isEqualTo(TEST_URL);
    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(request.getBody()).isNull();
    assertThat(request.getHeaders()).isNull();
    assertThat(request.getQueryParams()).isNull();
  }

  // -------------------------------------------------------------------------
  // Constructor tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("no-args constructor → all properties are null")
  void noArgsConstructorCreatesEmptyRequest() {
    HttpRequest<Void> request = new HttpRequest<>();

    assertThat(request.getUrl()).isNull();
    assertThat(request.getMethod()).isNull();
    assertThat(request.getBody()).isNull();
    assertThat(request.getHeaders()).isNull();
    assertThat(request.getQueryParams()).isNull();
  }

  @Test
  @DisplayName("all-args constructor → all properties set correctly")
  void allArgsConstructorSetsAllProperties() {
    Map<String, String> headers = Map.of("Content-Type", "application/json");
    Map<String, String> queryParams = Map.of("filter", "active");

    HttpRequest<String> request =
        new HttpRequest<>(TEST_URL, HttpMethod.PUT, REQUEST_BODY, headers, queryParams);

    assertThat(request.getUrl()).isEqualTo(TEST_URL);
    assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT);
    assertThat(request.getBody()).isEqualTo(REQUEST_BODY);
    assertThat(request.getHeaders()).isEqualTo(headers);
    assertThat(request.getQueryParams()).isEqualTo(queryParams);
  }

  // -------------------------------------------------------------------------
  // Generic type tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("generic body type → body type preserved correctly")
  void genericBodyTypePreservedCorrectly() {
    Integer numericBody = 42;
    HttpRequest<Integer> request =
        HttpRequest.<Integer>builder()
            .url(TEST_URL)
            .method(HttpMethod.PATCH)
            .body(numericBody)
            .build();

    assertThat(request.getBody()).isInstanceOf(Integer.class);
    assertThat(request.getBody()).isEqualTo(42);
  }

  @Test
  @DisplayName("void body type → body remains null")
  void voidBodyTypeRemainsNull() {
    HttpRequest<Void> request =
        HttpRequest.<Void>builder().url(TEST_URL).method(HttpMethod.DELETE).build();

    assertThat(request.getBody()).isNull();
  }
}
