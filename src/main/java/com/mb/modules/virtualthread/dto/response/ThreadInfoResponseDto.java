package com.mb.modules.virtualthread.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mb.modules.virtualthread.enums.ThreadMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Describes the thread that handled a request.
 *
 * @author pravin.sahu
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
public class ThreadInfoResponseDto {

  private boolean virtual;

  private ThreadMode threadType;

  private String threadName;

  private long threadId;

  /** {@code Thread.toString()}; for a virtual thread it also names the current carrier thread. */
  private String threadDescription;
}
