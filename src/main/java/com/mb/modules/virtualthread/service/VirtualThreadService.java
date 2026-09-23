package com.mb.modules.virtualthread.service;

import com.mb.modules.virtualthread.dto.response.ConcurrentRunResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;

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
   * Runs {@code tasks} simulated I/O waits concurrently using the given thread mode.
   *
   * @param mode platform pool or virtual-thread-per-task
   * @param tasks number of tasks
   * @param delayMs simulated wait per task
   * @param poolSize platform pool size (ignored for virtual)
   */
  ConcurrentRunResponseDto runConcurrentIo(ThreadMode mode, int tasks, long delayMs, int poolSize);
}
