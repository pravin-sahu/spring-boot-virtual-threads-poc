package com.mb.modules.virtualthread.service;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.constant.ResponseMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.virtualthread.benchmark.RunResult;
import com.mb.modules.virtualthread.benchmark.WorkloadRunner;
import com.mb.modules.virtualthread.benchmark.Workloads;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import com.mb.modules.virtualthread.dto.response.ComparisonResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ModeResultResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
import com.mb.modules.virtualthread.enums.WorkloadType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * @author pravin.sahu
 */
@Service
@Profile(VirtualThreadPocConfig.PROFILE)
@RequiredArgsConstructor
public class VirtualThreadServiceImpl implements VirtualThreadService {

  /** Upper bound for the theoretical duration of a platform run, since the endpoint is public. */
  static final long MAX_ESTIMATED_PLATFORM_RUN_MS = 60_000;

  /** CPU tasks are bounded separately: each one occupies a core until it finishes. */
  static final int MAX_CPU_TASKS = 64;

  private static final int DEFAULT_IO_TASKS = 2_000;
  private static final int DEFAULT_IO_POOL_SIZE = 100;

  private final WorkloadRunner workloadRunner;

  /** {@inheritDoc} */
  @Override
  public ThreadInfoResponseDto currentThreadInfo() {

    Thread current = Thread.currentThread();
    return ThreadInfoResponseDto.builder()
        .virtual(current.isVirtual())
        .threadType(current.isVirtual() ? ThreadMode.VIRTUAL : ThreadMode.PLATFORM)
        .threadName(current.getName())
        .threadId(current.threadId())
        .threadDescription(current.toString())
        .build();
  }

  /** {@inheritDoc} */
  @Override
  public IoSimulationResponseDto simulateIo(long delayMs) {

    long start = System.nanoTime();
    try {
      Workloads.simulateIoWait(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw interrupted();
    }
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    return IoSimulationResponseDto.builder()
        .requestedDelayMs(delayMs)
        .elapsedMs(elapsedMs)
        .thread(currentThreadInfo())
        .build();
  }

  /** {@inheritDoc} */
  @Override
  public ComparisonResponseDto compare(
      WorkloadType workload,
      Integer tasks,
      long delayMs,
      int primeLimit,
      Integer poolSize,
      int runs) {

    int processors = Runtime.getRuntime().availableProcessors();
    int taskCount = tasks != null ? tasks : defaultTasks(workload, processors);
    int pool = poolSize != null ? poolSize : defaultPoolSize(workload, processors);
    validate(workload, taskCount, delayMs, pool, runs);

    // The SAME Callable instance is used for both modes: that is what makes the comparison fair.
    Callable<Long> task =
        workload == WorkloadType.IO
            ? () -> Workloads.simulateIoWait(delayMs)
            : () -> Workloads.countPrimes(primeLimit);

    ModeResultResponseDto platform = execute(ThreadMode.PLATFORM, taskCount, pool, task, runs);
    ModeResultResponseDto virtual = execute(ThreadMode.VIRTUAL, taskCount, pool, task, runs);
    double speedup = (double) platform.getElapsedMs() / Math.max(1, virtual.getElapsedMs());

    return ComparisonResponseDto.builder()
        .workload(workload)
        .tasks(taskCount)
        .runs(runs)
        .delayMs(workload == WorkloadType.IO ? delayMs : null)
        .primeLimit(workload == WorkloadType.CPU ? primeLimit : null)
        .platform(platform)
        .virtual(virtual)
        .speedup(speedup)
        .sameWorkVerified(platform.getChecksum() == virtual.getChecksum())
        .summary(summary(workload, platform, virtual, speedup, processors))
        .build();
  }

  /** Runs one mode {@code runs} times and averages the elapsed time. */
  private ModeResultResponseDto execute(
      ThreadMode mode, int taskCount, int poolSize, Callable<Long> task, int runs) {

    List<Long> runsMs = new ArrayList<>(runs);
    RunResult last = null;
    int maxConcurrency = 0;
    for (int i = 0; i < runs; i++) {
      last = runOnce(mode, taskCount, poolSize, task);
      runsMs.add(last.elapsedMillis());
      maxConcurrency = Math.max(maxConcurrency, last.maxConcurrency());
    }

    long averageMs = Math.round(runsMs.stream().mapToLong(Long::longValue).average().orElseThrow());
    return ModeResultResponseDto.builder()
        .threadType(mode)
        .poolSize(last.poolSize())
        .elapsedMs(averageMs)
        .runsMs(runsMs)
        .throughputPerSecond(averageMs == 0 ? 0 : taskCount * 1_000.0 / averageMs)
        .maxObservedConcurrency(maxConcurrency)
        .tasksOnVirtualThreads(last.virtualThreadTasks())
        .checksum(last.checksum())
        .sampleThread(last.sampleThread())
        .build();
  }

  private RunResult runOnce(ThreadMode mode, int taskCount, int poolSize, Callable<Long> task) {
    try {
      return workloadRunner.run(mode, taskCount, poolSize, task);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw interrupted();
    } catch (ExecutionException e) {
      throw new AppException(
          ExceptionMessage.VIRTUAL_THREAD_RUN_FAILED,
          e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private static void validate(
      WorkloadType workload, int taskCount, long delayMs, int poolSize, int runs) {

    if (workload == WorkloadType.CPU && taskCount > MAX_CPU_TASKS) {
      throw badRequest(ExceptionMessage.VIRTUAL_THREAD_TOO_MANY_CPU_TASKS);
    }
    if (workload == WorkloadType.IO
        && ceilDiv(taskCount, poolSize) * delayMs * runs > MAX_ESTIMATED_PLATFORM_RUN_MS) {
      throw badRequest(ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG);
    }
  }

  private static String summary(
      WorkloadType workload,
      ModeResultResponseDto platform,
      ModeResultResponseDto virtual,
      double speedup,
      int processors) {

    if (workload == WorkloadType.IO) {
      return String.format(
          Locale.ROOT,
          ResponseMessage.VIRTUAL_THREAD_IO_SUMMARY,
          platform.getPoolSize(),
          platform.getMaxObservedConcurrency(),
          platform.getElapsedMs(),
          virtual.getMaxObservedConcurrency(),
          virtual.getElapsedMs(),
          speedup);
    }
    return String.format(
        Locale.ROOT,
        ResponseMessage.VIRTUAL_THREAD_CPU_SUMMARY,
        platform.getElapsedMs(),
        virtual.getElapsedMs(),
        processors,
        virtual.getMaxObservedConcurrency());
  }

  private static int defaultTasks(WorkloadType workload, int processors) {
    return workload == WorkloadType.IO ? DEFAULT_IO_TASKS : processors * 2;
  }

  private static int defaultPoolSize(WorkloadType workload, int processors) {
    return workload == WorkloadType.IO ? DEFAULT_IO_POOL_SIZE : processors;
  }

  private static long ceilDiv(long dividend, long divisor) {
    return (dividend + divisor - 1) / divisor;
  }

  private static AppException badRequest(String message) {
    return new AppException(message, null, HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST);
  }

  private static AppException interrupted() {
    return new AppException(
        ExceptionMessage.VIRTUAL_THREAD_RUN_INTERRUPTED,
        null,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.INTERNAL_SERVER_ERROR);
  }
}
