package com.mb.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.dto.response.ApiResponse;
import com.mb.common.dto.response.ErrorDetail;
import com.mb.common.util.ApiResponseBuilder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Directly invokes each exception handler method to verify the correct HTTP status code,
 * response structure, and error messages are produced. The test class and handler share the same
 * package, granting access to the protected {@link
 * org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler} overrides.
 * A {@link Spy} on the real {@link ApiResponseBuilder} ensures the actual response-building logic
 * runs while still allowing Mockito injection.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler – centralised exception-to-response mapping")
class GlobalExceptionHandlerTest {
  @Spy private ApiResponseBuilder apiResponseBuilder;
  @Mock private WebRequest webRequest;
  @InjectMocks private GlobalExceptionHandler handler;

  // -------------------------------------------------------------------------
  // appExceptionHandler – 4xx path (warn log)
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("4xx AppException → correct status, success=false, message forwarded")
  void appExceptionHandlerWith4xxStatusReturnsCorrectResponse() {
    AppException ex = new AppException("User not found", HttpStatus.NOT_FOUND);
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo("User not found");
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isNull();
            });
  }

  // -------------------------------------------------------------------------
  // appExceptionHandler – 5xx path (error log)
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("5xx AppException → 500 status and error-level logging branch taken")
  void appExceptionHandlerWith5xxStatusReturnsInternalServerError() {
    AppException ex =
        new AppException(
            ExceptionMessage.INTERNAL_SERVER_ERROR,
            "DB connection lost",
            HttpStatus.INTERNAL_SERVER_ERROR);
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo(ExceptionMessage.INTERNAL_SERVER_ERROR);
            });
  }

  @Test
  @DisplayName("AppException with VALIDATION_ERROR errorCode → errorCode included in response")
  void appExceptionHandlerWithValidationErrorCodeIncludesItInResponse() {
    AppException ex =
        new AppException(
            "Validation failure",
            "Field required",
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR);
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(body -> assertThat(body.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
  }

  @Test
  @DisplayName("AppException with UNAUTHORIZED errorCode → UNAUTHORIZED included in response")
  void appExceptionHandlerWithUnauthorizedErrorCodeIncludesItInResponse() {
    AppException ex =
        new AppException(
            "Authentication required", null, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(body -> assertThat(body.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
  }

  @Test
  @DisplayName("AppException with 502 Bad Gateway → error-level logging branch taken")
  void appExceptionHandlerWith502StatusTakesErrorLogPath() {
    AppException ex =
        new AppException("Bad Gateway", "Upstream service unavailable", HttpStatus.BAD_GATEWAY);
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    // 502 is a 5xx status, so should take error log path
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo("Bad Gateway");
            });
  }

  @Test
  @DisplayName(
      "AppException with null HttpStatus → null-check evaluates false, warn-log branch taken")
  void appExceptionHandlerWithNullHttpStatusTakesWarnLogPath() {
    AppException ex = new AppException("Something went wrong", null, null);
    ApiResponse<Object> stubBody =
        ApiResponse.<Object>builder().success(false).message("Something went wrong").build();
    doReturn(ResponseEntity.ok(stubBody))
        .when(apiResponseBuilder)
        .error(any(), isNull(), isNull(), isNull());
    ResponseEntity<ApiResponse<Object>> response = handler.appExceptionHandler(ex);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo("Something went wrong");
            });
  }

  // -------------------------------------------------------------------------
  // handleHttpRequestMethodNotSupported – 405
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("HttpRequestMethodNotSupportedException → 405, INVALID_REQUEST error code")
  @SuppressWarnings("rawtypes")
  void handleHttpRequestMethodNotSupportedReturnsMethodNotAllowed() {
    HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHttpRequestMethodNotSupported(
                ex, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getSuccess()).isFalse();
    assertThat(body.getMessage()).isEqualTo(ExceptionMessage.METHOD_NOT_ALLOWED);
    assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    assertThat(body.getErrors()).isNull();
  }

  // -------------------------------------------------------------------------
  // handleHttpMediaTypeNotSupported – 415
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("HttpMediaTypeNotSupportedException → 415, INVALID_REQUEST error code")
  @SuppressWarnings("rawtypes")
  void handleHttpMediaTypeNotSupportedReturnsUnsupportedMediaType() {
    HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("text/plain");
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHttpMediaTypeNotSupported(
                ex, new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getSuccess()).isFalse();
    assertThat(body.getMessage()).isEqualTo(ExceptionMessage.UNSUPPORTED_MEDIA_TYPE);
    assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    assertThat(body.getErrors()).isNull();
  }

  // -------------------------------------------------------------------------
  // nullPointerExceptionHandler
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("NullPointerException with message → 500, INTERNAL_SERVER_ERROR message")
  void nullPointerExceptionHandlerReturnsInternalServerError() {
    ResponseEntity<ApiResponse<Object>> response =
        handler.nullPointerExceptionHandler(new NullPointerException("null ref"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getMessage()).isEqualTo(ExceptionMessage.INTERNAL_SERVER_ERROR);
              assertThat(body.getErrors()).isNull();
              assertThat(body.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
            });
  }

  @Test
  @DisplayName("NullPointerException without message → still returns 500")
  void nullPointerExceptionHandlerWithNullMessageStillReturns500() {
    ResponseEntity<ApiResponse<Object>> response =
        handler.nullPointerExceptionHandler(new NullPointerException());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody())
        .isNotNull()
        .satisfies(body -> assertThat(body.getSuccess()).isFalse());
  }

  // -------------------------------------------------------------------------
  // handleTypeMismatch – generic TypeMismatchException
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("generic TypeMismatchException → 400, null field name, rejected value present")
  @SuppressWarnings("rawtypes")
  void handleTypeMismatchReturnsBadRequestWithNullFieldName() {
    TypeMismatchException ex = new TypeMismatchException("bad-value", java.util.UUID.class);
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleTypeMismatch(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getSuccess()).isFalse();
    assertThat(body.getMessage()).isEqualTo(ExceptionMessage.VALIDATION_ERROR);
    assertThat(body.getErrors()).hasSize(1);
    ErrorDetail detail = body.getErrors().get(0);
    assertThat(detail.getRejectedValue()).isEqualTo("bad-value");
    assertThat(detail.getMessage()).isEqualTo(ExceptionMessage.INVALID_VALUE);
    assertThat(detail.getField()).isNull();
  }

  // -------------------------------------------------------------------------
  // handleTypeMismatch – MethodArgumentTypeMismatchException
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("MethodArgumentTypeMismatchException → field name populated from parameter name")
  @SuppressWarnings("rawtypes")
  void handleTypeMismatchWithMethodArgumentTypeMismatchIncludesFieldName() throws Exception {
    Method method = String.class.getMethod("valueOf", Object.class);
    MethodParameter param = new MethodParameter(method, 0);
    MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException(
            "not-a-uuid", java.util.UUID.class, "userUuid", param, null);
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleTypeMismatch(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors()).hasSize(1);
    assertThat(body.getErrors().get(0).getField()).isEqualTo("userUuid");
    assertThat(body.getErrors().get(0).getRejectedValue()).isEqualTo("not-a-uuid");
  }

  // -------------------------------------------------------------------------
  // handleMethodArgumentNotValid
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("single field error → 400 with one ErrorDetail matching the FieldError")
  @SuppressWarnings("rawtypes")
  void handleMethodArgumentNotValidWithSingleFieldErrorReturnsBadRequest() {
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError =
        new FieldError("userDto", "email", "bad@", false, null, null, "must be a valid email");
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getSuccess()).isFalse();
    assertThat(body.getMessage()).isEqualTo(ExceptionMessage.VALIDATION_ERROR);
    assertThat(body.getErrors()).hasSize(1);
    ErrorDetail detail = body.getErrors().get(0);
    assertThat(detail.getField()).isEqualTo("email");
    assertThat(detail.getRejectedValue()).isEqualTo("bad@");
    assertThat(detail.getMessage()).isEqualTo("must be a valid email");
  }

  @Test
  @DisplayName("multiple field errors → all ErrorDetails returned")
  @SuppressWarnings("rawtypes")
  void handleMethodArgumentNotValidWithMultipleFieldErrorsReturnsAllDetails() {
    BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getFieldErrors())
        .thenReturn(
            List.of(
                new FieldError("dto", "firstName", null, false, null, null, "must not be blank"),
                new FieldError("dto", "email", "x", false, null, null, "must be a valid email")));
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors())
        .hasSize(2)
        .extracting(ErrorDetail::getField)
        .containsExactlyInAnyOrder("firstName", "email");
  }

  @Test
  @DisplayName("null rejected value → ErrorDetail still created with null rejectedValue")
  @SuppressWarnings("rawtypes")
  void handleMethodArgumentNotValidWithNullRejectedValueCreatesErrorDetail() {
    BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getFieldErrors())
        .thenReturn(
            List.of(
                new FieldError("dto", "firstName", null, false, null, null, "must not be blank")));
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors().get(0).getRejectedValue()).isNull();
    assertThat(body.getErrors().get(0).getMessage()).isEqualTo("must not be blank");
  }

  // -------------------------------------------------------------------------
  // handleHandlerMethodValidationException
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("validation result with error message → 400 with ErrorDetail from resolvable error")
  @SuppressWarnings("rawtypes")
  void handleHandlerMethodValidationExceptionReturnsBadRequestWithErrors() throws Exception {
    Method method = String.class.getMethod("valueOf", Object.class);
    MethodParameter param = mock(MethodParameter.class);
    when(param.getParameterName()).thenReturn("name");
    org.springframework.context.MessageSourceResolvable resolvableError =
        mock(org.springframework.context.MessageSourceResolvable.class);
    when(resolvableError.getDefaultMessage()).thenReturn("must not be blank");
    ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
    when(validationResult.getMethodParameter()).thenReturn(param);
    when(validationResult.getArgument()).thenReturn("");
    when(validationResult.getResolvableErrors()).thenReturn(List.of(resolvableError));
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    when(ex.getMessage()).thenReturn("Validation failed");
    when(ex.getMethod()).thenReturn(method);
    when(ex.getParameterValidationResults()).thenReturn(List.of(validationResult));
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHandlerMethodValidationException(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getSuccess()).isFalse();
    assertThat(body.getMessage()).isEqualTo(ExceptionMessage.VALIDATION_ERROR);
    assertThat(body.getErrors()).hasSize(1);
    ErrorDetail detail = body.getErrors().get(0);
    assertThat(detail.getField()).isEqualTo("name");
    assertThat(detail.getMessage()).isEqualTo("must not be blank");
    assertThat(detail.getRejectedValue()).isEqualTo("");
  }

  @Test
  @DisplayName("no resolvable errors → falls back to INVALID_VALUE message")
  @SuppressWarnings("rawtypes")
  void handleHandlerMethodValidationExceptionWithNoResolvableErrorsFallsBackToInvalidValue()
      throws Exception {
    Method method = String.class.getMethod("valueOf", Object.class);
    MethodParameter param = mock(MethodParameter.class);
    when(param.getParameterName()).thenReturn("id");
    ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
    when(validationResult.getMethodParameter()).thenReturn(param);
    when(validationResult.getArgument()).thenReturn("x");
    when(validationResult.getResolvableErrors()).thenReturn(List.of());
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    when(ex.getMessage()).thenReturn("Validation failed");
    when(ex.getMethod()).thenReturn(method);
    when(ex.getParameterValidationResults()).thenReturn(List.of(validationResult));
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHandlerMethodValidationException(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors()).hasSize(1);
    assertThat(body.getErrors().get(0).getMessage()).isEqualTo(ExceptionMessage.INVALID_VALUE);
  }

  @Test
  @DisplayName("multiple validation results → one ErrorDetail per result")
  @SuppressWarnings("rawtypes")
  void handleHandlerMethodValidationExceptionWithMultipleResultsReturnsOneDetailPerResult()
      throws Exception {
    Method method = String.class.getMethod("valueOf", Object.class);
    MethodParameter param1 = mock(MethodParameter.class);
    when(param1.getParameterName()).thenReturn("firstName");
    org.springframework.context.MessageSourceResolvable error1 =
        mock(org.springframework.context.MessageSourceResolvable.class);
    when(error1.getDefaultMessage()).thenReturn("must not be blank");
    ParameterValidationResult result1 = mock(ParameterValidationResult.class);
    when(result1.getMethodParameter()).thenReturn(param1);
    when(result1.getArgument()).thenReturn("");
    when(result1.getResolvableErrors()).thenReturn(List.of(error1));
    MethodParameter param2 = mock(MethodParameter.class);
    when(param2.getParameterName()).thenReturn("age");
    org.springframework.context.MessageSourceResolvable error2 =
        mock(org.springframework.context.MessageSourceResolvable.class);
    when(error2.getDefaultMessage()).thenReturn("must be positive");
    ParameterValidationResult result2 = mock(ParameterValidationResult.class);
    when(result2.getMethodParameter()).thenReturn(param2);
    when(result2.getArgument()).thenReturn(-1);
    when(result2.getResolvableErrors()).thenReturn(List.of(error2));
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    when(ex.getMessage()).thenReturn("Validation failed");
    when(ex.getMethod()).thenReturn(method);
    when(ex.getParameterValidationResults()).thenReturn(List.of(result1, result2));
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHandlerMethodValidationException(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors())
        .hasSize(2)
        .extracting(ErrorDetail::getField)
        .containsExactlyInAnyOrder("firstName", "age");
  }

  // -------------------------------------------------------------------------
  // Additional coverage tests
  // -------------------------------------------------------------------------
  @Test
  @DisplayName(
      "handleHandlerMethodValidationException: null parameter name → null field in ErrorDetail")
  @SuppressWarnings("rawtypes")
  void handleHandlerMethodValidationExceptionWithNullParameterNameCreatesNullField()
      throws Exception {
    Method method = String.class.getMethod("valueOf", Object.class);
    MethodParameter param = mock(MethodParameter.class);
    when(param.getParameterName()).thenReturn(null);
    org.springframework.context.MessageSourceResolvable resolvableError =
        mock(org.springframework.context.MessageSourceResolvable.class);
    when(resolvableError.getDefaultMessage()).thenReturn("validation failed");
    ParameterValidationResult validationResult = mock(ParameterValidationResult.class);
    when(validationResult.getMethodParameter()).thenReturn(param);
    when(validationResult.getArgument()).thenReturn("invalid");
    when(validationResult.getResolvableErrors()).thenReturn(List.of(resolvableError));
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    when(ex.getMessage()).thenReturn("Validation failed");
    when(ex.getMethod()).thenReturn(method);
    when(ex.getParameterValidationResults()).thenReturn(List.of(validationResult));
    ResponseEntity response =
        Objects.requireNonNull(
            handler.handleHandlerMethodValidationException(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest));
    ApiResponse<?> body = Objects.requireNonNull((ApiResponse<?>) response.getBody());
    assertThat(body.getErrors()).hasSize(1);
    assertThat(body.getErrors().get(0).getField()).isNull();
    assertThat(body.getErrors().get(0).getMessage()).isEqualTo("validation failed");
  }
}
