package com.mb.modules.virtualthread.unit.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mb.modules.virtualthread.benchmark.BenchmarkResult;
import com.mb.modules.virtualthread.benchmark.BenchmarkRunner;
import com.mb.modules.virtualthread.benchmark.BenchmarkRuns;
import com.mb.modules.virtualthread.benchmark.Workloads;
import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asserts correctness only (counts, thread type, concurrency bounds) — never timings. */
@DisplayName("BenchmarkRunner – executes the same workload on platform and virtual threads")
class BenchmarkRunnerTest {

  private static final int TASKS = 10;
  private static final int POOL_SIZE = 2;

  private final BenchmarkRunner runner = new BenchmarkRunner();

  @Test
  @DisplayName("platform mode never exceeds the pool size and uses no virtual threads")
  void platformModeIsBoundedByPoolSize() throws Exception {
    BenchmarkResult result =
        runner.run(ThreadMode.PLATFORM, TASKS, POOL_SIZE, () -> Workloads.simulateIoWait(5));

    assertThat(result.mode()).isEqualTo(ThreadMode.PLATFORM);
    assertThat(result.poolSize()).isEqualTo(POOL_SIZE);
    assertThat(result.taskCount()).isEqualTo(TASKS);
    assertThat(result.maxConcurrency()).isBetween(1, POOL_SIZE);
    assertThat(result.virtualThreadTasks()).isZero();
    assertThat(result.checksum()).isEqualTo(TASKS);
  }

  @Test
  @DisplayName("virtual mode runs every task on its own virtual thread, all concurrently")
  void virtualModeRunsAllTasksConcurrently() throws Exception {
    // Each task waits until all TASKS tasks have started: only possible if they run concurrently.
    CountDownLatch allStarted = new CountDownLatch(TASKS);

    BenchmarkResult result =
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
  }

  @Test
  @DisplayName("same CPU workload produces the same checksum in both modes")
  void sameWorkloadProducesSameChecksum() throws Exception {
    BenchmarkResult platform =
        runner.run(ThreadMode.PLATFORM, TASKS, POOL_SIZE, () -> Workloads.countPrimes(1_000));
    BenchmarkResult virtual =
        runner.run(ThreadMode.VIRTUAL, TASKS, POOL_SIZE, () -> Workloads.countPrimes(1_000));

    assertThat(platform.checksum()).isEqualTo(virtual.checksum()).isEqualTo(168L * TASKS);
  }

  @Test
  @DisplayName("runRepeated discards warm-up runs and keeps only measured runs")
  void runRepeatedKeepsMeasuredRunsOnly() throws Exception {
    AtomicInteger executions = new AtomicInteger();

    BenchmarkRuns runs =
        runner.runRepeated(
            ThreadMode.VIRTUAL, 3, POOL_SIZE, () -> (long) executions.incrementAndGet(), 2, 3);

    assertThat(runs.runs()).hasSize(3);
    assertThat(executions).hasValue(15);
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
