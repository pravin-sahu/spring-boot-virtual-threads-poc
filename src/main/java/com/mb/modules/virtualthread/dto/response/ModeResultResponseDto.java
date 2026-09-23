package com.mb.modules.virtualthread.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * One side of a comparison: how one thread mode handled the workload.
 *
 * @author pravin.sahu
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
public class ModeResultResponseDto {

  private ThreadMode threadType;

  /**
   * Platform pool size; absent for {@link ThreadMode#VIRTUAL}, which creates one thread per task.
   */
  private Integer poolSize;

  /** Average across all runs. */
  private long elapsedMs;

  private List<Long> runsMs;

  private double throughputPerSecond;

  private int maxObservedConcurrency;

  private long tasksOnVirtualThreads;

  private long checksum;

  /** {@code Thread.toString()} of the thread that ran the first task. */
  private String sampleThread;
}
