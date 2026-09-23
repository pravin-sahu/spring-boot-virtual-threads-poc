package com.mb.modules.virtualthread.benchmark;

import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.concurrent.TimeUnit;

/**
 * Measurements from a single execution of a batch of tasks.
 *
 * @param mode thread model used
 * @param taskCount number of tasks submitted
 * @param poolSize platform pool size, or {@code null} for virtual threads (one thread per task)
 * @param elapsedNanos wall-clock time from executor creation until every task finished
 * @param maxConcurrency highest number of tasks observed running at the same time
 * @param virtualThreadTasks number of tasks that observed {@code
 *     Thread.currentThread().isVirtual()}
 * @param checksum sum of all task results; identical across modes when the workload is identical
 * @author pravin.sahu
 */
public record BenchmarkResult(
    ThreadMode mode,
    int taskCount,
    Integer poolSize,
    long elapsedNanos,
    int maxConcurrency,
    long virtualThreadTasks,
    long checksum) {

  public long elapsedMillis() {
    return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
  }

  public double throughputPerSecond() {
    return taskCount / (elapsedNanos / 1_000_000_000.0);
  }
}
