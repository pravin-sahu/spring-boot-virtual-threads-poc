package com.mb.modules.virtualthread.service;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.virtualthread.benchmark.BenchmarkResult;
import com.mb.modules.virtualthread.benchmark.BenchmarkRunner;
import com.mb.modules.virtualthread.benchmark.Workloads;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import com.mb.modules.virtualthread.dto.response.ConcurrentRunResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
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

  private final BenchmarkRunner benchmarkRunner;

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
  public ConcurrentRunResponseDto runConcurrentIo(
      ThreadMode mode, int tasks, long delayMs, int poolSize) {

    if (mode == ThreadMode.PLATFORM
        && ((long) tasks + poolSize - 1) / poolSize * delayMs > MAX_ESTIMATED_PLATFORM_RUN_MS) {
      throw new AppException(
          ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG,
          null,
          HttpStatus.BAD_REQUEST,
          ErrorCode.INVALID_REQUEST);
    }

    BenchmarkResult result;
    try {
      result = benchmarkRunner.run(mode, tasks, poolSize, () -> Workloads.simulateIoWait(delayMs));
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

    return ConcurrentRunResponseDto.builder()
        .mode(result.mode())
        .tasks(result.taskCount())
        .delayMs(delayMs)
        .poolSize(result.poolSize())
        .elapsedMs(result.elapsedMillis())
        .throughputPerSecond(result.throughputPerSecond())
        .maxObservedConcurrency(result.maxConcurrency())
        .tasksOnVirtualThreads(result.virtualThreadTasks())
        .build();
  }

  private static AppException interrupted() {
    return new AppException(
        ExceptionMessage.VIRTUAL_THREAD_RUN_INTERRUPTED,
        null,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.INTERNAL_SERVER_ERROR);
  }
}
