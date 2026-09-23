package com.mb.modules.virtualthread.service;

import com.mb.modules.virtualthread.dto.response.ComparisonResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.WorkloadType;

/**
 * Virtual thread PoC operations.
 *
 * @author pravin.sahu
 */
public interface VirtualThreadService {

  /** Describes the thread executing the current request. */
  ThreadInfoResponseDto currentThreadInfo();

  /**
   * Simulates an I/O wait ({@code Thread.sleep}) on the request thread.
   *
   * @param delayMs simulated wait in milliseconds
   */
  IoSimulationResponseDto simulateIo(long delayMs);

  /**
   * Runs the same workload twice — once on a fixed platform pool, once on one virtual thread per
   * task — and reports both sides.
   *
   * @param workload waiting ({@code IO}) or computing ({@code CPU})
   * @param tasks number of tasks, or {@code null} for the default of the chosen workload
   * @param delayMs simulated wait per task, used by {@code IO}
   * @param primeLimit prime-counting limit per task, used by {@code CPU}
   * @param poolSize platform pool size, or {@code null} for the default of the chosen workload
   * @param runs how many times to repeat each mode; the times are averaged
   */
  ComparisonResponseDto compare(
      WorkloadType workload,
      Integer tasks,
      long delayMs,
      int primeLimit,
      Integer poolSize,
      int runs);
}
