package com.mb.modules.virtualthread.benchmark;

import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;

/**
 * Executes the same task {@code taskCount} times on either a fixed platform-thread pool or a
 * virtual-thread-per-task executor, and measures the batch.
 *
 * <p>Plain Java: no Spring or database dependency, so it can be used from a {@code main} method.
 *
 * @author pravin.sahu
 */
public class BenchmarkRunner {

  /**
   * Runs one batch.
   *
   * @param mode thread model
   * @param taskCount number of tasks
   * @param poolSize platform pool size (ignored for {@link ThreadMode#VIRTUAL})
   * @param task the unit of work; its results are summed into the checksum
   * @return measurements for this batch
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws ExecutionException if any task throws
   */
  public BenchmarkResult run(ThreadMode mode, int taskCount, int poolSize, Callable<Long> task)
      throws InterruptedException, ExecutionException {

    ConcurrencyTracker tracker = new ConcurrencyTracker();
    LongAdder virtualThreadTasks = new LongAdder();
    List<Callable<Long>> tasks = new ArrayList<>(taskCount);
    for (int i = 0; i < taskCount; i++) {
      tasks.add(instrument(task, tracker, virtualThreadTasks));
    }

    long checksum = 0;
    // Timing covers executor creation, thread creation and shutdown for both modes alike.
    long start = System.nanoTime();
    try (ExecutorService executor = newExecutor(mode, poolSize)) {
      for (Future<Long> future : executor.invokeAll(tasks)) {
        checksum += future.get();
      }
    }
    long elapsedNanos = System.nanoTime() - start;

    return new BenchmarkResult(
        mode,
        taskCount,
        mode == ThreadMode.PLATFORM ? poolSize : null,
        elapsedNanos,
        tracker.maxObserved(),
        virtualThreadTasks.sum(),
        checksum);
  }

  /**
   * Runs {@code warmupRuns} discarded batches followed by {@code measuredRuns} recorded batches.
   * Warm-up lets the JIT compile the hot paths so the measured runs are more stable.
   */
  public BenchmarkRuns runRepeated(
      ThreadMode mode,
      int taskCount,
      int poolSize,
      Callable<Long> task,
      int warmupRuns,
      int measuredRuns)
      throws InterruptedException, ExecutionException {

    for (int i = 0; i < warmupRuns; i++) {
      run(mode, taskCount, poolSize, task);
    }
    List<BenchmarkResult> results = new ArrayList<>(measuredRuns);
    for (int i = 0; i < measuredRuns; i++) {
      results.add(run(mode, taskCount, poolSize, task));
    }
    return new BenchmarkRuns(results);
  }

  private static ExecutorService newExecutor(ThreadMode mode, int poolSize) {
    return switch (mode) {
      case PLATFORM -> Executors.newFixedThreadPool(poolSize);
      case VIRTUAL -> Executors.newVirtualThreadPerTaskExecutor();
    };
  }

  private static Callable<Long> instrument(
      Callable<Long> task, ConcurrencyTracker tracker, LongAdder virtualThreadTasks) {
    return () -> {
      tracker.enter();
      try {
        if (Thread.currentThread().isVirtual()) {
          virtualThreadTasks.increment();
        }
        return task.call();
      } finally {
        tracker.exit();
      }
    };
  }
}
