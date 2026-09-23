package com.mb.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Custom http success response model to return paginated data with pagination details.
 *
 * @author rohit.kavthekar
 * @param <T>
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponse<T> {

  private List<T> content;

  private Integer pageNumber;

  private Integer pageSize;

  private Long totalElements;

  private Integer totalPages;

  private Boolean isFirst;

  private Boolean isLast;
}
