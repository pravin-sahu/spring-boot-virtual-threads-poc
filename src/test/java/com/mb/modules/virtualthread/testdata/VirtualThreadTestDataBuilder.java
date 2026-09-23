package com.mb.modules.virtualthread.testdata;

import com.mb.modules.virtualthread.benchmark.BenchmarkResult;
import com.mb.modules.virtualthread.dto.response.ConcurrentRunResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;

/**
 * Central test-data factory for virtual thread PoC tests.
 *
 * @author pravin.sahu
 */
public class VirtualThreadTestDataBuilder {

  public static final String INFO_URL = "/v1/virtual-threads/info";
  public static final String IO_URL = "/v1/virtual-threads/io";
  public static final String CONCURRENT_URL = "/v1/virtual-threads/concurrent";
  public static final String VIRTUAL_THREAD_NAME = "tomcat-handler-0";
  public static final long DELAY_MS = 50;
  public static final int TASKS = 20;
  public static final int POOL_SIZE = 5;

  private VirtualThreadTestDataBuilder() {}

  public static ThreadInfoResponseDto buildVirtualThreadInfo() {
    return ThreadInfoResponseDto.builder()
        .virtual(true)
        .threadType(ThreadMode.VIRTUAL)
        .threadName(VIRTUAL_THREAD_NAME)
        .threadId(42)
        .threadDescription("VirtualThread[#42]/runnable@ForkJoinPool-1-worker-1")
        .build();
  }

  public static IoSimulationResponseDto buildIoSimulation() {
    return IoSimulationResponseDto.builder()
        .requestedDelayMs(DELAY_MS)
        .elapsedMs(DELAY_MS)
        .thread(buildVirtualThreadInfo())
        .build();
  }

  public static ConcurrentRunResponseDto buildConcurrentRun(ThreadMode mode) {
    return ConcurrentRunResponseDto.builder()
        .mode(mode)
        .tasks(TASKS)
        .delayMs(DELAY_MS)
        .poolSize(mode == ThreadMode.PLATFORM ? POOL_SIZE : null)
        .elapsedMs(DELAY_MS)
        .throughputPerSecond(400.0)
        .maxObservedConcurrency(TASKS)
        .tasksOnVirtualThreads(mode == ThreadMode.VIRTUAL ? TASKS : 0)
        .build();
  }

  /** Result with {@code elapsedMillis} milliseconds for {@link #TASKS} tasks. */
  public static BenchmarkResult buildResult(ThreadMode mode, long elapsedMillis) {
    return new BenchmarkResult(
        mode,
        TASKS,
        mode == ThreadMode.PLATFORM ? POOL_SIZE : null,
        elapsedMillis * 1_000_000,
        mode == ThreadMode.PLATFORM ? POOL_SIZE : TASKS,
        mode == ThreadMode.VIRTUAL ? TASKS : 0,
        TASKS);
  }
}
