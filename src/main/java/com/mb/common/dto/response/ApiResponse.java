package com.mb.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mb.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Custom http success response model to return any type of generic data with status code and
 * message.
 *
 * @author rohit.kavthekar
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ApiResponse<D> {

  private String message;

  private Boolean success;

  private D data;

  private Instant timestamp;

  private List<ErrorDetail> errors;

  private ErrorCode errorCode;
}
