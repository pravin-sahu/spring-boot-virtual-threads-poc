package com.mb.modules.virtualthread.benchmark;

import java.util.List;

/**
 * The measured (non warm-up) runs of one scenario, with simple aggregate statistics.
 *
 * @param runs measured runs, in execution order
 * @author pravin.sahu
 */
public record BenchmarkRuns(List<BenchmarkResult> runs) {

  public BenchmarkRuns {
    if (runs.isEmpty()) {
      throw new IllegalArgumentException("At least one measured run is required");
    }
    runs = List.copyOf(runs);
  }

  public BenchmarkResult first() {
    return runs.getFirst();
  }

  public double averageMillis() {
    return runs.stream().mapToLong(BenchmarkResult::elapsedNanos).average().orElseThrow()
        / 1_000_000.0;
  }

  public long minMillis() {
    return runs.stream().mapToLong(BenchmarkResult::elapsedMillis).min().orElseThrow();
  }

  public long maxMillis() {
    return runs.stream().mapToLong(BenchmarkResult::elapsedMillis).max().orElseThrow();
  }

  public double averageThroughput() {
    return runs.stream().mapToDouble(BenchmarkResult::throughputPerSecond).average().orElseThrow();
  }

  public int maxConcurrency() {
    return runs.stream().mapToInt(BenchmarkResult::maxConcurrency).max().orElseThrow();
  }
}
