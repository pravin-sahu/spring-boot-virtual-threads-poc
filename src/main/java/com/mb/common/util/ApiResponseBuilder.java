package com.mb.common.util;

import com.mb.common.dto.response.ApiResponse;
import com.mb.common.dto.response.ErrorDetail;
import com.mb.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Component class used to build response entity for returning success, error and validation
 * responses. It provides methods to build response entity for success and error responses with
 * appropriate http status and response body. It helps to maintain consistency in the response
 * structure across the application and reduces code duplication in controllers and exception
 * handlers by centralizing the response building logic in one place.
 *
 * @author rohit.kavthekar
 */
@Component
public class ApiResponseBuilder {

  /**
   * Build http success response entity.
   *
   * @param <T> Generic type response class
   * @param message success message
   * @param data actual response data
   * @param httpStatus http status
   * @return {@link ResponseEntity}
   */
  public <D> ResponseEntity<ApiResponse<D>> success(String message, D data, HttpStatus httpStatus) {

    ApiResponse<D> response =
        ApiResponse.<D>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(Instant.now())
            .build();

    return new ResponseEntity<>(response, httpStatus);
  }

  /**
   * Build error response with http status.
   *
   * @param <T> Generic type response class
   * @param message error message
   * @param errors list of error details
   * @param errorCode error code for the error response
   * @param httpStatus http status
   * @return {@link ResponseEntity}
   */
  public <D> ResponseEntity<ApiResponse<D>> error(
      String message, List<ErrorDetail> errors, ErrorCode errorCode, HttpStatus httpStatus) {

    ApiResponse<D> response =
        ApiResponse.<D>builder()
            .success(false)
            .message(message)
            .timestamp(Instant.now())
            .errors(errors)
            .errorCode(errorCode)
            .build();

    return new ResponseEntity<>(response, httpStatus);
  }
}
