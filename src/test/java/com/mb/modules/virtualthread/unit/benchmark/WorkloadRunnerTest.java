package com.mb.modules.virtualthread.unit.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mb.modules.virtualthread.benchmark.RunResult;
import com.mb.modules.virtualthread.benchmark.WorkloadRunner;
import com.mb.modules.virtualthread.benchmark.Workloads;
import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts correctness only (counts, thread type, concurrency bounds) — never timings. */
@DisplayName("WorkloadRunner – runs the same workload on platform and virtual threads")
class WorkloadRunnerTest {

  private static final int TASKS = 10;
  private static final int POOL_SIZE = 2;

  private final WorkloadRunner runner = new WorkloadRunner();

  @Test
  @DisplayName("platform mode never exceeds the pool size and uses no virtual threads")
  void platformModeIsBoundedByPoolSize() throws Exception {
    RunResult result =
        runner.run(ThreadMode.PLATFORM, TASKS, POOL_SIZE, () -> Workloads.simulateIoWait(5));

    assertThat(result.mode()).isEqualTo(ThreadMode.PLATFORM);
    assertThat(result.poolSize()).isEqualTo(POOL_SIZE);
    assertThat(result.taskCount()).isEqualTo(TASKS);
    assertThat(result.maxConcurrency()).isBetween(1, POOL_SIZE);
    assertThat(result.virtualThreadTasks()).isZero();
    assertThat(result.checksum()).isEqualTo(TASKS);
    assertThat(result.sampleThread()).doesNotContain("VirtualThread");
    assertThat(result.elapsedMillis()).isNotNegative();
    assertThat(result.throughputPerSecond()).isPositive();
  }

  @Test
  @DisplayName("virtual mode runs every task on its own virtual thread, all concurrently")
  void virtualModeRunsAllTasksConcurrently() throws Exception {
    // Each task waits until all TASKS tasks have started: only possible if they run concurrently.
    CountDownLatch allStarted = new CountDownLatch(TASKS);

    RunResult result =
        runner.run(
            ThreadMode.VIRTUAL,
            TASKS,
            POOL_SIZE,
            () -> {
              allStarted.countDown();
              return allStarted.await(10, TimeUnit.SECONDS) ? 1L : 0L;
            });

    assertThat(result.poolSize()).isNull();
    assertThat(result.maxConcurrency()).isEqualTo(TASKS);
    assertThat(result.virtualThreadTasks()).isEqualTo(TASKS);
    assertThat(result.checksum()).isEqualTo(TASKS);
    assertThat(result.sampleThread()).startsWith("VirtualThread[");
  }

  @Test
  @DisplayName("same CPU workload produces the same checksum in both modes")
  void sameWorkloadProducesSameChecksum() throws Exception {
    RunResult platform =
        runner.run(ThreadMode.PLATFORM, TASKS, POOL_SIZE, () -> Workloads.countPrimes(1_000));
    RunResult virtual =
        runner.run(ThreadMode.VIRTUAL, TASKS, POOL_SIZE, () -> Workloads.countPrimes(1_000));

    assertThat(platform.checksum()).isEqualTo(virtual.checksum()).isEqualTo(168L * TASKS);
  }

  @Test
  @DisplayName("a failing task surfaces as ExecutionException")
  void failingTaskThrowsExecutionException() {
    assertThatThrownBy(
            () ->
                runner.run(
                    ThreadMode.VIRTUAL,
                    TASKS,
                    POOL_SIZE,
                    () -> {
                      throw new IllegalStateException("boom");
                    }))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(IllegalStateException.class);
  }
}
