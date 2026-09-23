package com.mb.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link AppException}.
 *
 * <p>Verifies that all three constructors correctly populate the exception fields: message, detail,
 * httpStatus, and errorCode. Ensures null defaults are applied where no value is supplied.
 *
 * @author rohit.kavthekar
 */
@DisplayName("AppException – custom runtime exception construction")
class AppExceptionTest {

  private static final String MESSAGE = "Something went wrong";
  private static final String DETAIL = "Detailed error description";

  // -------------------------------------------------------------------------
  // Full constructor (message, detail, httpStatus, errorCode)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("full constructor sets every field with the supplied values")
  void constructorWithAllFieldsSetsEveryField() {
    AppException ex =
        new AppException(MESSAGE, DETAIL, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);

    assertThat(ex.getMessage()).isEqualTo(MESSAGE);
    assertThat(ex.getDetail()).isEqualTo(DETAIL);
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  @DisplayName("full constructor supports UNAUTHORIZED error code")
  void constructorWithUnauthorizedErrorCode() {
    AppException ex =
        new AppException(MESSAGE, DETAIL, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);

    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
  }

  // -------------------------------------------------------------------------
  // Constructor without errorCode
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("constructor without errorCode leaves errorCode null")
  void constructorWithoutErrorCodeSetsNullErrorCode() {
    AppException ex = new AppException(MESSAGE, DETAIL, HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(ex.getMessage()).isEqualTo(MESSAGE);
    assertThat(ex.getDetail()).isEqualTo(DETAIL);
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(ex.getErrorCode()).isNull();
  }

  // -------------------------------------------------------------------------
  // Short constructor (message, httpStatus only)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("short constructor leaves detail and errorCode null")
  void constructorWithMessageAndStatusOnlySetsNullDetailAndErrorCode() {
    AppException ex = new AppException(MESSAGE, HttpStatus.NOT_FOUND);

    assertThat(ex.getMessage()).isEqualTo(MESSAGE);
    assertThat(ex.getDetail()).isNull();
    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex.getErrorCode()).isNull();
  }

  // -------------------------------------------------------------------------
  // Type hierarchy
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("is a RuntimeException so callers are not forced to declare it")
  void appExceptionIsRuntimeException() {
    AppException ex = new AppException(MESSAGE, HttpStatus.BAD_REQUEST);

    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
