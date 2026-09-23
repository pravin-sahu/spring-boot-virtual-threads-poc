package com.mb.common.exception;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.dto.response.ApiResponse;
import com.mb.common.dto.response.ErrorDetail;
import com.mb.common.util.ApiResponseBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * A global exception handler class to handle exception thrown by application. It intercepts the
 * final step for creating the response entity object. Added different exception handler methods to
 * catch and throw exception response. Uses response builder util class to build response entity
 * class.
 *
 * @author rohit.kavthekar
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private final ApiResponseBuilder apiResponseBuilder;

  /**
   * App exception handler.
   *
   * @param <T> Generic type exception handler
   * @param appException accepts app exception
   * @return {@link ResponseEntity}
   */
  @ExceptionHandler(AppException.class)
  public <D> ResponseEntity<ApiResponse<D>> appExceptionHandler(AppException appException) {

    if (appException.getHttpStatus() != null && appException.getHttpStatus().is5xxServerError()) {
      log.error(
          "AppException caught - Message: {}, ErrorCode: {}, HttpStatus: {}, Detail: {}",
          appException.getMessage(),
          appException.getErrorCode(),
          appException.getHttpStatus(),
          appException.getDetail());
    } else {
      log.warn(
          "AppException caught - Message: {}, ErrorCode: {}, HttpStatus: {}, Detail: {}",
          appException.getMessage(),
          appException.getErrorCode(),
          appException.getHttpStatus(),
          appException.getDetail());
    }

    return apiResponseBuilder.error(
        appException.getMessage(), null, appException.getErrorCode(), appException.getHttpStatus());
  }

  /**
   * Null pointer exception handler.
   *
   * @param nullPointerException null pointer exception class
   * @return {@link ResponseEntity}
   */
  @ExceptionHandler(NullPointerException.class)
  public <D> ResponseEntity<ApiResponse<D>> nullPointerExceptionHandler(
      NullPointerException nullPointerException) {

    log.error(
        "NullPointerException caught - Message: {}",
        nullPointerException.getMessage() != null
            ? nullPointerException.getMessage()
            : "No message provided");
    log.debug("NullPointerException stack trace: ", nullPointerException);

    return apiResponseBuilder.error(
        ExceptionMessage.INTERNAL_SERVER_ERROR,
        null,
        ErrorCode.INTERNAL_SERVER_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handles HTTP method not supported (405).
   *
   * @param ex the method not supported exception
   * @param headers HTTP headers
   * @param status resolved HTTP status
   * @param request the current web request
   * @return {@link ResponseEntity}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected ResponseEntity handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    log.warn("HttpRequestMethodNotSupportedException caught - Method: {}", ex.getMethod());

    return apiResponseBuilder.error(
        ExceptionMessage.METHOD_NOT_ALLOWED,
        null,
        ErrorCode.INVALID_REQUEST,
        HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Handles unsupported media type (415).
   *
   * @param ex the unsupported media type exception
   * @param headers HTTP headers
   * @param status resolved HTTP status
   * @param request the current web request
   * @return {@link ResponseEntity}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected ResponseEntity handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    log.warn("HttpMediaTypeNotSupportedException caught - ContentType: {}", ex.getContentType());

    return apiResponseBuilder.error(
        ExceptionMessage.UNSUPPORTED_MEDIA_TYPE,
        null,
        ErrorCode.INVALID_REQUEST,
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  /**
   * Handles type-mismatch failures that occur when Spring MVC cannot convert a path variable or
   * request parameter to the declared method parameter type (e.g. a malformed UUID string bound to
   * a {@link java.util.UUID} parameter). Without this override, {@link
   * ResponseEntityExceptionHandler} returns an RFC 7807 Problem Details body; this replaces that
   * with the application's standard error envelope.
   *
   * @param ex the type mismatch exception
   * @param headers HTTP headers
   * @param status resolved HTTP status
   * @param request the current web request
   * @return {@link ResponseEntity}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected ResponseEntity handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    log.warn(
        "TypeMismatchException caught - Field: {}, RejectedValue: {}",
        ex instanceof MethodArgumentTypeMismatchException matme ? matme.getName() : "unknown",
        ex.getValue());

    String fieldName =
        ex instanceof MethodArgumentTypeMismatchException matme ? matme.getName() : null;

    List<ErrorDetail> errors =
        List.of(
            ErrorDetail.builder()
                .field(fieldName)
                .rejectedValue(ex.getValue())
                .message(ExceptionMessage.INVALID_VALUE)
                .build());

    return apiResponseBuilder.error(
        ExceptionMessage.VALIDATION_ERROR, errors, null, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler method validation exception handler.
   *
   * @param ex Handler method validation exception
   * @param headers http available headers
   * @param status http status
   * @param request http web request
   * @return {@link ResponseEntity}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected ResponseEntity handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    log.warn(
        "HandlerMethodValidationException caught - Message: {}, Method: {}",
        ex.getMessage(),
        ex.getMethod().getName());

    List<ErrorDetail> errors =
        ex.getParameterValidationResults().stream()
            .map(
                result ->
                    ErrorDetail.builder()
                        .field(result.getMethodParameter().getParameterName())
                        .rejectedValue(result.getArgument())
                        .message(
                            result.getResolvableErrors().stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse(ExceptionMessage.INVALID_VALUE))
                        .build())
            .toList();

    return apiResponseBuilder.error(
        ExceptionMessage.VALIDATION_ERROR, errors, null, HttpStatus.BAD_REQUEST);
  }

  /**
   * Validation exception response handler.
   *
   * @param ex Validation exception
   * @param headers http available headers
   * @param status http status
   * @param request http web request
   * @return {@link ResponseEntity}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected ResponseEntity handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    log.warn("MethodArgumentNotValidException caught - Message: {}", ex.getMessage());

    List<ErrorDetail> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldErr ->
                    ErrorDetail.builder()
                        .field(fieldErr.getField())
                        .rejectedValue(fieldErr.getRejectedValue())
                        .message(fieldErr.getDefaultMessage())
                        .build())
            .toList();

    return apiResponseBuilder.error(
        ExceptionMessage.VALIDATION_ERROR, errors, null, HttpStatus.BAD_REQUEST);
  }
}
