package com.mb.common.constant;

/**
 * A constant class for storing all http response messages. Use message.properties file to store and
 * get message using key.
 *
 * @author rohit.kavthekar
 */
public class ResponseMessage {

  private ResponseMessage() {}

  // General
  public static final String SUCCESS = "Success";
  public static final String CREATED = "Created successfully";
  public static final String UPDATED = "Updated successfully";
  public static final String DELETED = "Deleted successfully";

  // Virtual Thread PoC — summary templates
  public static final String VIRTUAL_THREAD_IO_SUMMARY =
      "A pool of %d platform threads ran at most %d tasks at a time and took %d ms. Virtual threads"
          + " ran %d tasks at the same time and took %d ms (%.1fx faster). Thread.sleep simulates"
          + " waiting; it is not real database or network I/O.";
  public static final String VIRTUAL_THREAD_CPU_SUMMARY =
      "Platform threads took %d ms, virtual threads %d ms. CPU work is limited by the %d available"
          + " cores: virtual threads never ran more than %d tasks at once, because a computing"
          + " thread never gives up its carrier. Virtual threads add no CPU capacity.";
}
