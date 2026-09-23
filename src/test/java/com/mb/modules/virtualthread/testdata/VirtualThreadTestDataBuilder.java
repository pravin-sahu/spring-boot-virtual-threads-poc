package com.mb.modules.virtualthread.testdata;

import com.mb.modules.virtualthread.benchmark.RunResult;
import com.mb.modules.virtualthread.dto.response.ComparisonResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ModeResultResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
import com.mb.modules.virtualthread.enums.WorkloadType;
import java.util.List;

/**
 * Central test-data factory for virtual thread PoC tests.
 *
 * @author pravin.sahu
 */
public class VirtualThreadTestDataBuilder {

  public static final String INFO_URL = "/v1/virtual-threads/info";
  public static final String IO_URL = "/v1/virtual-threads/io";
  public static final String COMPARE_URL = "/v1/virtual-threads/compare";
  public static final String VIRTUAL_THREAD_NAME = "tomcat-handler-0";
  public static final String PLATFORM_SAMPLE_THREAD = "Thread[#31,pool-1-thread-1,5,main]";
  public static final String VIRTUAL_SAMPLE_THREAD =
      "VirtualThread[#42]/runnable@ForkJoinPool-1-worker-1";
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
        .threadDescription(VIRTUAL_SAMPLE_THREAD)
        .build();
  }

  public static IoSimulationResponseDto buildIoSimulation() {
    return IoSimulationResponseDto.builder()
        .requestedDelayMs(DELAY_MS)
        .elapsedMs(DELAY_MS)
        .thread(buildVirtualThreadInfo())
        .build();
  }

  /** Result with {@code elapsedMillis} milliseconds for {@link #TASKS} tasks. */
  public static RunResult buildRunResult(ThreadMode mode, long elapsedMillis) {
    boolean virtual = mode == ThreadMode.VIRTUAL;
    return new RunResult(
        mode,
        TASKS,
        virtual ? null : POOL_SIZE,
        elapsedMillis * 1_000_000,
        virtual ? TASKS : POOL_SIZE,
        virtual ? TASKS : 0,
        TASKS,
        virtual ? VIRTUAL_SAMPLE_THREAD : PLATFORM_SAMPLE_THREAD);
  }

  public static ModeResultResponseDto buildModeResult(ThreadMode mode, long elapsedMs) {
    boolean virtual = mode == ThreadMode.VIRTUAL;
    return ModeResultResponseDto.builder()
        .threadType(mode)
        .poolSize(virtual ? null : POOL_SIZE)
        .elapsedMs(elapsedMs)
        .runsMs(List.of(elapsedMs))
        .throughputPerSecond(TASKS * 1_000.0 / elapsedMs)
        .maxObservedConcurrency(virtual ? TASKS : POOL_SIZE)
        .tasksOnVirtualThreads(virtual ? TASKS : 0)
        .checksum(TASKS)
        .sampleThread(virtual ? VIRTUAL_SAMPLE_THREAD : PLATFORM_SAMPLE_THREAD)
        .build();
  }

  public static ComparisonResponseDto buildComparison(WorkloadType workload) {
    return ComparisonResponseDto.builder()
        .workload(workload)
        .tasks(TASKS)
        .runs(1)
        .delayMs(workload == WorkloadType.IO ? DELAY_MS : null)
        .primeLimit(workload == WorkloadType.CPU ? 1_000 : null)
        .platform(buildModeResult(ThreadMode.PLATFORM, 200))
        .virtual(buildModeResult(ThreadMode.VIRTUAL, 50))
        .speedup(4.0)
        .sameWorkVerified(true)
        .summary("summary text")
        .build();
  }
}
