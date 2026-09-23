package com.mb.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response model class used to return validation field error messages and value.
 *
 * @author rohit.kavthekar
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ErrorDetail {

  private String field;

  private String message;

  private Object rejectedValue;
}
