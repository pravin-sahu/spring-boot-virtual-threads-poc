package com.mb.common.constant;

/**
 * A constant class for storing all REST API URLs, base URL and its version.
 *
 * @author rohit.kavthekar
 */
public final class ApiEndpoint {

  /** Private constructor to prevent instantiation of this utility class. */
  private ApiEndpoint() {}

  // User Controller
  public static final String USERS = "{version}/users";
  public static final String USER_BY_UUID = "/{userUuid}";

  // Virtual Thread PoC Controller
  public static final String VIRTUAL_THREADS = "{version}/virtual-threads";
  public static final String VIRTUAL_THREADS_INFO = "/info";
  public static final String VIRTUAL_THREADS_IO = "/io";
  public static final String VIRTUAL_THREADS_COMPARE = "/compare";
  public static final String VIRTUAL_THREADS_PUBLIC_PATTERN = "/v1/virtual-threads/**";
}
