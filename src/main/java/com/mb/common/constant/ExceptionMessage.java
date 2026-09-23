package com.mb.common.constant;

/**
 * A constant class for storing all exception's or error's messages. Use exception.properties file
 * to store and get message using key.
 *
 * @author rohit.kavthekar
 */
public class ExceptionMessage {

  private ExceptionMessage() {}

  // General
  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String DUPLICATE_RESOURCE = "Resource already exists";
  public static final String METHOD_NOT_ALLOWED = "Request method not supported";
  public static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported media type";

  // Validation
  public static final String VALIDATION_ERROR = "Validation failure";
  public static final String INVALID_VALUE = "Invalid value";

  // User
  public static final String USER_NOT_FOUND = "User not found";
  public static final String USER_NOT_FOUND_BY_UUID = "User not found with uuid: ";

  // Role
  public static final String ROLE_NOT_FOUND = "Role not found with name: ";

  // UserIdentity
  public static final String IDENTITY_NOT_FOUND = "User identity not found for provider: ";

  // Virtual Thread PoC
  public static final String VIRTUAL_THREAD_RUN_TOO_LONG =
      "Requested platform run would exceed 60 seconds; reduce tasks or delayMs, or raise poolSize";
  public static final String VIRTUAL_THREAD_RUN_INTERRUPTED = "Virtual thread demo was interrupted";
  public static final String VIRTUAL_THREAD_RUN_FAILED = "Virtual thread demo task failed";
}
