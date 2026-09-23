package com.mb.modules.virtualthread.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mb.modules.virtualthread.enums.ThreadMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Measurements of a batch of simulated I/O waits executed inside the running application.
 *
 * @author pravin.sahu
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
public class ConcurrentRunResponseDto {

  private ThreadMode mode;

  private int tasks;

  private long delayMs;

  /** Platform pool size; absent for {@link ThreadMode#VIRTUAL}. */
  private Integer poolSize;

  private long elapsedMs;

  private double throughputPerSecond;

  private int maxObservedConcurrency;

  private long tasksOnVirtualThreads;
}
