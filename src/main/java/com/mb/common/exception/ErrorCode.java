package com.mb.common.exception;

/**
 * Enum class to define error codes for the application. It can be extended to include more error
 * codes as needed.
 *
 * @author rohit.kavthekar
 */
public enum ErrorCode {
  VALIDATION_ERROR,
  UNAUTHORIZED,
  FORBIDDEN,
  RESOURCE_NOT_FOUND,
  CONFLICT,
  DUPLICATE_RESOURCE,
  INVALID_REQUEST,
  INTERNAL_SERVER_ERROR
}
