package com.mb.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.mb.common.dto.response.ApiResponse;
import com.mb.common.dto.response.ErrorDetail;
import com.mb.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link ApiResponseBuilder}.
 *
 * <p>Verifies correct construction of standardized API responses for both success and error
 * scenarios. Tests response structure, HTTP status codes, and proper handling of optional fields
 * like data payloads, error lists, and error codes.
 *
 * @author rohit.kavthekar
 */
@DisplayName("ApiResponseBuilder – standardised response envelope construction")
class ApiResponseBuilderTest {

  private static final String SUCCESS_MESSAGE = "Success";
  private static final String CREATED_MESSAGE = "Created";
  private static final String VALIDATION_FAILURE_MESSAGE = "Validation failure";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";
  private static final String NOT_FOUND_MESSAGE = "Not Found";
  private static final String TEST_PAYLOAD = "test-payload";
  private static final String TEST_DATA = "data";
  private static final String UUID_FIELD = "uuid";
  private static final String INVALID_UUID_MESSAGE = "Invalid UUID format";
  private static final String BAD_VALUE = "bad-value";

  private ApiResponseBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new ApiResponseBuilder();
  }

  // -------------------------------------------------------------------------
  // Success responses
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("success with data → success=true, message, data, timestamp set; errors null")
  void successWithDataBuildsCorrectResponse() {
    ResponseEntity<ApiResponse<String>> response =
        builder.success(SUCCESS_MESSAGE, TEST_PAYLOAD, HttpStatus.OK);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isTrue();
              assertThat(body.getMessage()).isEqualTo(SUCCESS_MESSAGE);
              assertThat(body.getData()).isEqualTo(TEST_PAYLOAD);
              assertThat(body.getTimestamp()).isNotNull();
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isNull();
            });
  }

  @Test
  @DisplayName("success with null data → data field is null, other fields intact")
  void successWithNullDataOmitsDataField() {
    ResponseEntity<ApiResponse<String>> response =
        builder.success(SUCCESS_MESSAGE, null, HttpStatus.OK);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isTrue();
              assertThat(body.getMessage()).isEqualTo(SUCCESS_MESSAGE);
              assertThat(body.getData()).isNull();
              assertThat(body.getTimestamp()).isNotNull();
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isNull();
            });
  }

  @Test
  @DisplayName("success with HTTP 201 Created → status code is 201")
  void successWithCreatedStatusReturnsHttpCreated() {
    ResponseEntity<ApiResponse<String>> response =
        builder.success(CREATED_MESSAGE, TEST_DATA, HttpStatus.CREATED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isTrue();
              assertThat(body.getMessage()).isEqualTo(CREATED_MESSAGE);
              assertThat(body.getData()).isEqualTo(TEST_DATA);
              assertThat(body.getTimestamp()).isNotNull();
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isNull();
            });
  }

  // -------------------------------------------------------------------------
  // Error responses
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("error with error list → success=false, errors list and errorCode set")
  void errorWithErrorListBuildsCorrectErrorResponse() {
    List<ErrorDetail> errors =
        List.of(
            ErrorDetail.builder()
                .field(UUID_FIELD)
                .message(INVALID_UUID_MESSAGE)
                .rejectedValue(BAD_VALUE)
                .build());

    ResponseEntity<ApiResponse<Void>> response =
        builder.error(
            VALIDATION_FAILURE_MESSAGE, errors, ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo(VALIDATION_FAILURE_MESSAGE);
              assertThat(body.getErrors()).hasSize(1);
              assertThat(body.getErrors().get(0).getField()).isEqualTo(UUID_FIELD);
              assertThat(body.getErrors().get(0).getRejectedValue()).isEqualTo(BAD_VALUE);
              assertThat(body.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(body.getData()).isNull();
              assertThat(body.getTimestamp()).isNotNull();
            });
  }

  @Test
  @DisplayName("error with null errors and null errorCode → errors and errorCode fields are null")
  void errorWithNullErrorsBuildsErrorResponseWithoutErrorList() {
    ResponseEntity<ApiResponse<Void>> response =
        builder.error(INTERNAL_SERVER_ERROR_MESSAGE, null, null, HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isNull();
              assertThat(body.getData()).isNull();
              assertThat(body.getTimestamp()).isNotNull();
            });
  }

  @Test
  @DisplayName("error with null errorCode → errorCode field is null")
  void errorWithNullErrorCodeOmitsErrorCode() {
    ResponseEntity<ApiResponse<Void>> response =
        builder.error(NOT_FOUND_MESSAGE, null, null, HttpStatus.NOT_FOUND);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getErrorCode()).isNull();
              assertThat(body.getData()).isNull();
              assertThat(body.getTimestamp()).isNotNull();
            });
  }

  @Test
  @DisplayName("error with UNAUTHORIZED errorCode → errorCode is UNAUTHORIZED")
  void errorWithUnauthorizedErrorCodeSetsIt() {
    ResponseEntity<ApiResponse<Void>> response =
        builder.error("Unauthorized", null, ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(body -> assertThat(body.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
  }
}
