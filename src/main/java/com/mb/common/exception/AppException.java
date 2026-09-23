package com.mb.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The custom exception class extends runtime exception for throwing customized exception with
 * message, error code and http status according to needs.
 *
 * @author rohit.kavthekar
 */
@Getter
public class AppException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus httpStatus;
  private final ErrorCode errorCode;
  private final String detail;

  /**
   * Constructs new custom exception with message, error description and http status.
   *
   * @param message short message
   * @param detail descriptive message
   * @param httpStatus http status
   * @param errorCode error code
   */
  public AppException(String message, String detail, HttpStatus httpStatus, ErrorCode errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.detail = detail;
    this.errorCode = errorCode;
  }

  /**
   * Constructs new custom exception with message, error description and http status.
   *
   * @param message short message
   * @param detail descriptive message
   * @param httpStatus http status
   */
  public AppException(String message, String detail, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
    this.detail = detail;
    this.errorCode = null;
  }

  /**
   * Constructs new custom exception with message and http status.
   *
   * @param message short message
   * @param httpStatus http status
   */
  public AppException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
    this.detail = null;
    this.errorCode = null;
  }
}
