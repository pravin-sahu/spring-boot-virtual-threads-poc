package com.mb.modules.virtualthread.unit.service;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.POOL_SIZE;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.TASKS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildRunResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.virtualthread.benchmark.WorkloadRunner;
import com.mb.modules.virtualthread.dto.response.ComparisonResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
import com.mb.modules.virtualthread.enums.WorkloadType;
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
@DisplayName("VirtualThreadServiceImpl – thread info, simulated I/O and mode comparison")
class VirtualThreadServiceImplTest {

  @Mock private WorkloadRunner workloadRunner;

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
  // compare
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("IO comparison runs both modes with the same task and reports the speed-up")
  @SuppressWarnings("unchecked")
  void compareIoRunsBothModes() throws Exception {
    when(workloadRunner.run(eq(ThreadMode.PLATFORM), eq(TASKS), eq(POOL_SIZE), any()))
        .thenReturn(buildRunResult(ThreadMode.PLATFORM, 200));
    when(workloadRunner.run(eq(ThreadMode.VIRTUAL), eq(TASKS), eq(POOL_SIZE), any()))
        .thenReturn(buildRunResult(ThreadMode.VIRTUAL, 50));

    ComparisonResponseDto result = service.compare(WorkloadType.IO, TASKS, 10, 1_000, POOL_SIZE, 1);

    assertThat(result.getWorkload()).isEqualTo(WorkloadType.IO);
    assertThat(result.getTasks()).isEqualTo(TASKS);
    assertThat(result.getDelayMs()).isEqualTo(10);
    assertThat(result.getPrimeLimit()).isNull();
    assertThat(result.getPlatform().getPoolSize()).isEqualTo(POOL_SIZE);
    assertThat(result.getPlatform().getElapsedMs()).isEqualTo(200);
    assertThat(result.getVirtual().getPoolSize()).isNull();
    assertThat(result.getVirtual().getTasksOnVirtualThreads()).isEqualTo(TASKS);
    assertThat(result.getSpeedup()).isCloseTo(4.0, within(0.001));
    assertThat(result.isSameWorkVerified()).isTrue();
    assertThat(result.getSummary()).contains("Thread.sleep simulates waiting");

    // The same Callable must be handed to both modes: that is the fairness guarantee.
    ArgumentCaptor<Callable<Long>> task = ArgumentCaptor.forClass(Callable.class);
    verify(workloadRunner, times(2))
        .run(any(ThreadMode.class), eq(TASKS), eq(POOL_SIZE), task.capture());
    assertThat(task.getAllValues().get(0)).isSameAs(task.getAllValues().get(1));
    assertThat(task.getValue().call()).isEqualTo(1L);
  }

  @Test
  @DisplayName("CPU comparison uses the prime workload and explains the core limit")
  @SuppressWarnings("unchecked")
  void compareCpuUsesPrimeWorkload() throws Exception {
    when(workloadRunner.run(any(), anyInt(), anyInt(), any()))
        .thenReturn(
            buildRunResult(ThreadMode.PLATFORM, 100), buildRunResult(ThreadMode.VIRTUAL, 100));

    ComparisonResponseDto result = service.compare(WorkloadType.CPU, 8, 0, 1_000, 4, 1);

    assertThat(result.getPrimeLimit()).isEqualTo(1_000);
    assertThat(result.getDelayMs()).isNull();
    assertThat(result.getSpeedup()).isCloseTo(1.0, within(0.001));
    assertThat(result.getSummary()).contains("add no CPU capacity");

    ArgumentCaptor<Callable<Long>> task = ArgumentCaptor.forClass(Callable.class);
    verify(workloadRunner, times(2)).run(any(), anyInt(), anyInt(), task.capture());
    assertThat(task.getValue().call()).isEqualTo(168L); // primes below 1000
  }

  @Test
  @DisplayName("null tasks/poolSize fall back to the defaults of the workload")
  void compareUsesDefaults() throws Exception {
    when(workloadRunner.run(any(), eq(2_000), eq(100), any()))
        .thenReturn(
            buildRunResult(ThreadMode.PLATFORM, 10), buildRunResult(ThreadMode.VIRTUAL, 10));

    ComparisonResponseDto result = service.compare(WorkloadType.IO, null, 0, 1_000, null, 1);

    assertThat(result.getTasks()).isEqualTo(2_000);
    verify(workloadRunner, times(2)).run(any(), eq(2_000), eq(100), any());
  }

  @Test
  @DisplayName("runs=2 repeats each mode and averages the elapsed time")
  void compareAveragesRepeatedRuns() throws Exception {
    when(workloadRunner.run(any(), anyInt(), anyInt(), any()))
        .thenReturn(
            buildRunResult(ThreadMode.PLATFORM, 100),
            buildRunResult(ThreadMode.PLATFORM, 300),
            buildRunResult(ThreadMode.VIRTUAL, 20),
            buildRunResult(ThreadMode.VIRTUAL, 40));

    ComparisonResponseDto result = service.compare(WorkloadType.IO, TASKS, 1, 1_000, POOL_SIZE, 2);

    assertThat(result.getRuns()).isEqualTo(2);
    assertThat(result.getPlatform().getRunsMs()).containsExactly(100L, 300L);
    assertThat(result.getPlatform().getElapsedMs()).isEqualTo(200);
    assertThat(result.getVirtual().getElapsedMs()).isEqualTo(30);
    verify(workloadRunner, times(4)).run(any(), anyInt(), anyInt(), any());
  }

  @Test
  @DisplayName("IO run estimated above 60s → 400 INVALID_REQUEST, runner not called")
  void compareRejectsTooLongIoRun() {
    assertThatThrownBy(() -> service.compare(WorkloadType.IO, 5_000, 2_000, 1_000, 1, 1))
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG)
        .extracting("httpStatus", "errorCode")
        .containsExactly(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST);

    verifyNoInteractions(workloadRunner);
  }

  @Test
  @DisplayName("more than 64 CPU tasks → 400 INVALID_REQUEST, runner not called")
  void compareRejectsTooManyCpuTasks() {
    assertThatThrownBy(() -> service.compare(WorkloadType.CPU, 65, 0, 1_000, 8, 1))
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_TOO_MANY_CPU_TASKS);

    verifyNoInteractions(workloadRunner);
  }

  @Test
  @DisplayName("task failure → 500 AppException")
  void compareWhenTaskFailsThrowsAppException() throws Exception {
    when(workloadRunner.run(any(), anyInt(), anyInt(), any()))
        .thenThrow(new ExecutionException(new IllegalStateException("boom")));

    assertThatThrownBy(() -> service.compare(WorkloadType.IO, TASKS, 1, 1_000, POOL_SIZE, 1))
        .isInstanceOf(AppException.class)
        .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_FAILED)
        .extracting("httpStatus")
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("interrupted while waiting → 503 AppException and interrupt flag restored")
  void compareWhenInterruptedThrowsAppException() throws Exception {
    when(workloadRunner.run(any(), anyInt(), anyInt(), any()))
        .thenThrow(new InterruptedException());

    try {
      assertThatThrownBy(() -> service.compare(WorkloadType.IO, TASKS, 1, 1_000, POOL_SIZE, 1))
          .isInstanceOf(AppException.class)
          .hasMessage(ExceptionMessage.VIRTUAL_THREAD_RUN_INTERRUPTED);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      // Clear the flag so it does not leak into other tests on this thread.
      Thread.interrupted();
    }
  }
}
