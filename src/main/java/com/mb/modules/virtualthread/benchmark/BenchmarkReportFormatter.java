package com.mb.modules.virtualthread.benchmark;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Formats benchmark results as human-readable text.
 *
 * @author pravin.sahu
 */
public final class BenchmarkReportFormatter {

  private BenchmarkReportFormatter() {}

  public static String ioHeader(
      int taskCount, long ioWaitMillis, int poolSize, int warmupRuns, int measuredRuns) {
    long platformLowerBound = ceilDiv(taskCount, poolSize) * ioWaitMillis;
    return format(
        "=== I/O-BOUND BENCHMARK ===%n%n"
            + "Tasks: %d%n"
            + "Simulated I/O wait: %d ms per task (Thread.sleep: simulates waiting only, not real"
            + " I/O)%n"
            + "Warm-up runs: %d | Measured runs: %d%n"
            + "Theoretical minimum, platform: ceil(%d / %d) x %d ms = %d ms%n"
            + "Theoretical minimum, virtual:  ~%d ms (all tasks wait at the same time)%n",
        taskCount,
        ioWaitMillis,
        warmupRuns,
        measuredRuns,
        taskCount,
        poolSize,
        ioWaitMillis,
        platformLowerBound,
        ioWaitMillis);
  }

  public static String cpuHeader(
      int taskCount, int primeLimit, int processors, int warmupRuns, int measuredRuns) {
    return format(
        "=== CPU-BOUND BENCHMARK ===%n%n"
            + "Tasks: %d%n"
            + "Work per task: count primes up to %d (trial division, deterministic)%n"
            + "Available processors: %d%n"
            + "Warm-up runs: %d | Measured runs: %d%n",
        taskCount, primeLimit, processors, warmupRuns, measuredRuns);
  }

  public static String section(String title, BenchmarkRuns runs) {
    BenchmarkResult first = runs.first();
    String poolSize =
        first.poolSize() == null
            ? "n/a (one new virtual thread per task)"
            : String.valueOf(first.poolSize());
    String runMillis =
        runs.runs().stream()
            .map(run -> String.valueOf(run.elapsedMillis()))
            .collect(Collectors.joining(", "));
    return format(
        "%n%s:%n"
            + "  Pool size: %s%n"
            + "  Total time (avg): %.1f ms  [min %d, max %d]%n"
            + "  Runs (ms): %s%n"
            + "  Throughput (avg): %.1f tasks/s%n"
            + "  Max observed concurrency: %d%n"
            + "  Tasks run on virtual threads: %d / %d%n"
            + "  Checksum: %d%n",
        title,
        poolSize,
        runs.averageMillis(),
        runs.minMillis(),
        runs.maxMillis(),
        runMillis,
        runs.averageThroughput(),
        runs.maxConcurrency(),
        first.virtualThreadTasks(),
        first.taskCount(),
        first.checksum());
  }

  public static String comparison(BenchmarkRuns platform, BenchmarkRuns virtual) {
    boolean checksumsMatch = platform.first().checksum() == virtual.first().checksum();
    return format(
        "%nComparison: platform avg / virtual avg = %.2fx%n" + "Checksums match: %s%n",
        platform.averageMillis() / virtual.averageMillis(),
        checksumsMatch ? "yes (same work done in both modes)" : "NO - workloads differ");
  }

  public static String cpuNote(int processors) {
    return format(
        "%nNote: CPU-bound work is limited by the %d available cores. Virtual threads run on a"
            + " small pool of carrier threads (one per core by default) and never give up their"
            + " carrier while computing, so they cannot add CPU capacity. Expect similar times.%n",
        processors);
  }

  private static long ceilDiv(long dividend, long divisor) {
    return (dividend + divisor - 1) / divisor;
  }

  private static String format(String template, Object... args) {
    return String.format(Locale.ROOT, template, args);
  }
}
