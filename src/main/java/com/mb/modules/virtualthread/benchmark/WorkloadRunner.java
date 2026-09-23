package com.mb.modules.virtualthread.benchmark;

import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Runs the same task {@code taskCount} times, either on a fixed pool of platform threads or on one
 * virtual thread per task, and measures the batch.
 *
 * <p>The choice between the two executors in {@link #newExecutor} is the entire experiment:
 * everything else is identical for both modes.
 *
 * @author pravin.sahu
 */
public class WorkloadRunner {

  /**
   * Runs one batch and measures it.
   *
   * @param mode thread model
   * @param taskCount number of tasks
   * @param poolSize platform pool size (ignored for {@link ThreadMode#VIRTUAL})
   * @param task the unit of work; its results are summed into the checksum
   * @return measurements for this batch
   * @throws InterruptedException if the calling thread is interrupted while waiting
   * @throws ExecutionException if any task throws
   */
  public RunResult run(ThreadMode mode, int taskCount, int poolSize, Callable<Long> task)
      throws InterruptedException, ExecutionException {

    ConcurrencyTracker tracker = new ConcurrencyTracker();
    LongAdder virtualThreadTasks = new LongAdder();
    AtomicReference<String> sampleThread = new AtomicReference<>();

    List<Callable<Long>> tasks = new ArrayList<>(taskCount);
    for (int i = 0; i < taskCount; i++) {
      tasks.add(instrument(task, tracker, virtualThreadTasks, sampleThread));
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

    return new RunResult(
        mode,
        taskCount,
        mode == ThreadMode.PLATFORM ? poolSize : null,
        elapsedNanos,
        tracker.maxObserved(),
        virtualThreadTasks.sum(),
        checksum,
        sampleThread.get());
  }

  private static ExecutorService newExecutor(ThreadMode mode, int poolSize) {
    return switch (mode) {
      case PLATFORM -> Executors.newFixedThreadPool(poolSize);
      case VIRTUAL -> Executors.newVirtualThreadPerTaskExecutor();
    };
  }

  /** Wraps the workload so every task counts itself in and out, and reports its thread. */
  private static Callable<Long> instrument(
      Callable<Long> task,
      ConcurrencyTracker tracker,
      LongAdder virtualThreadTasks,
      AtomicReference<String> sampleThread) {
    return () -> {
      tracker.enter();
      try {
        Thread current = Thread.currentThread();
        if (current.isVirtual()) {
          virtualThreadTasks.increment();
        }
        sampleThread.compareAndSet(null, current.toString());
        return task.call();
      } finally {
        tracker.exit();
      }
    };
  }
}
