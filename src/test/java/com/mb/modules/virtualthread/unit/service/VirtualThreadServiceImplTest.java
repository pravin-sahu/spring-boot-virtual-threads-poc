package com.mb.modules.virtualthread.unit.service;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.DELAY_MS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.POOL_SIZE;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.TASKS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.virtualthread.benchmark.BenchmarkRunner;
import com.mb.modules.virtualthread.dto.response.ConcurrentRunResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
import com.mb.modules.virtualthread.service.VirtualThreadServiceImpl;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("VirtualThreadServiceImpl – thread info, simulated I/O and concurrent runs")
class VirtualThreadServiceImplTest {

  @Mock private BenchmarkRunner benchmarkRunner;

  @InjectMocks private VirtualThreadServiceImpl service;

  // -------------------------------------------------------------------------
  // currentThreadInfo
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("on a platform thread → virtual=false, type PLATFORM")
  void currentThreadInfoOnPlatformThread() {
    ThreadInfoResponseDto info = service.currentThreadInfo();

    assertThat(info.isVirtual()).isFalse();
    assertThat(info.getThreadType()).isEqualTo(ThreadMode.PLATFORM);
    assertThat(info.getThreadName()).isEqualTo(Thread.currentThread().getName());
    assertThat(info.getThreadId()).isEqualTo(Thread.currentThread().threadId());
  }

  @Test
  @DisplayName("on a virtual thread → virtual=true, type VIRTUAL, description names the carrier")
  void currentThreadInfoOnVirtualThread() throws InterruptedException {
    AtomicReference<ThreadInfoResponseDto> info = new AtomicReference<>();

    Thread.ofVirtual().name("vt-test").start(() -> info.set(service.currentThreadInfo())).join();

    assertThat(info.get().isVirtual()).isTrue();
    assertThat(info.get().getThreadType()).isEqualTo(ThreadMode.VIRTUAL);
    assertThat(info.get().getThreadName()).isEqualTo("vt-test");
    assertThat(info.get().getThreadDescription()).startsWith("VirtualThread[");
  }

  // -------------------------------------------------------------------------
  // simulateIo
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("simulateIo waits and reports requested and elapsed time")
  void simulateIoReportsElapsedTime() {
    IoSimulationResponseDto result = service.simulateIo(20);

    assertThat(result.getRequestedDelayMs()).isEqualTo(20);
    assertThat(result.getElapsedMs()).isGreaterThanOrEqualTo(20);
    assertThat(result.getThread()).isNotNull();
  }

  @Test
  @DisplayName("simulateIo on an interrupted thread → 503 AppException and interrupt flag restored")
  void simulateIoWhenInterruptedThrowsAppException() throws InterruptedException {
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    AtomicReference<Boolean> flagRestored = new AtomicReference<>();

    Thread.ofVirtual()
        .start(
            () -> {
              Thread.currentThread().interrupt();
              try {
                service.simulateIo(1_000);
              } catch (AppException e) {
                thrown.set(e);
                flagRestored.set(Thread.currentThread().isInterrupted());
              }
            })
        .join();

    assertThat(thrown.get())
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_INTERRUPTED);
    assertThat(((AppException) thrown.get()).getHttpStatus())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(flagRestored.get()).isTrue();
  }

  // -------------------------------------------------------------------------
  // runConcurrentIo
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("maps the benchmark result and passes a simulated I/O task to the runner")
  @SuppressWarnings("unchecked")
  void runConcurrentIoMapsResult() throws Exception {
    when(benchmarkRunner.run(eq(ThreadMode.PLATFORM), eq(TASKS), eq(POOL_SIZE), any()))
        .thenReturn(buildResult(ThreadMode.PLATFORM, 200));

    ConcurrentRunResponseDto result =
        service.runConcurrentIo(ThreadMode.PLATFORM, TASKS, 0, POOL_SIZE);

    assertThat(result.getMode()).isEqualTo(ThreadMode.PLATFORM);
    assertThat(result.getTasks()).isEqualTo(TASKS);
    assertThat(result.getPoolSize()).isEqualTo(POOL_SIZE);
    assertThat(result.getElapsedMs()).isEqualTo(200);
    assertThat(result.getThroughputPerSecond()).isEqualTo(TASKS / 0.2);
    assertThat(result.getMaxObservedConcurrency()).isEqualTo(POOL_SIZE);
    assertThat(result.getTasksOnVirtualThreads()).isZero();

    ArgumentCaptor<Callable<Long>> task = ArgumentCaptor.forClass(Callable.class);
    verify(benchmarkRunner).run(eq(ThreadMode.PLATFORM), eq(TASKS), eq(POOL_SIZE), task.capture());
    assertThat(task.getValue().call()).isEqualTo(1L);
  }

  @Test
  @DisplayName("platform run estimated above 60s → 400 INVALID_REQUEST, runner not called")
  void runConcurrentIoRejectsTooLongPlatformRun() {
    assertThatThrownBy(() -> service.runConcurrentIo(ThreadMode.PLATFORM, 10_000, 1_000, 1))
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG)
        .extracting("httpStatus", "errorCode")
        .containsExactly(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST);

    verifyNoInteractions(benchmarkRunner);
  }

  @Test
  @DisplayName("virtual run with the same parameters is allowed (tasks wait concurrently)")
  void runConcurrentIoAllowsLargeVirtualRun() throws Exception {
    when(benchmarkRunner.run(eq(ThreadMode.VIRTUAL), eq(10_000), anyInt(), any()))
        .thenReturn(buildResult(ThreadMode.VIRTUAL, 1_000));

    ConcurrentRunResponseDto result = service.runConcurrentIo(ThreadMode.VIRTUAL, 10_000, 1_000, 1);

    assertThat(result.getMode()).isEqualTo(ThreadMode.VIRTUAL);
    assertThat(result.getPoolSize()).isNull();
  }

  @Test
  @DisplayName("task failure → 500 AppException")
  void runConcurrentIoWhenTaskFailsThrowsAppException() throws Exception {
    when(benchmarkRunner.run(any(), anyInt(), anyInt(), any()))
        .thenThrow(new ExecutionException(new IllegalStateException("boom")));

    assertThatThrownBy(() -> service.runConcurrentIo(ThreadMode.VIRTUAL, TASKS, DELAY_MS, 1))
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_FAILED)
        .extracting("httpStatus")
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("interrupted while waiting → 503 AppException and interrupt flag restored")
  void runConcurrentIoWhenInterruptedThrowsAppException() throws Exception {
    when(benchmarkRunner.run(any(), anyInt(), anyInt(), any()))
        .thenThrow(new InterruptedException());

    try {
      assertThatThrownBy(() -> service.runConcurrentIo(ThreadMode.VIRTUAL, TASKS, DELAY_MS, 1))
          .isInstanceOf(AppException.class)
          .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_INTERRUPTED);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      // Clear the flag so it does not leak into other tests on this thread.
      Thread.interrupted();
    }
  }
}
