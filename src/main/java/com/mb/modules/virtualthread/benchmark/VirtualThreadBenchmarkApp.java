package com.mb.modules.virtualthread.benchmark;

import com.mb.modules.virtualthread.enums.ThreadMode;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Command-line entry point for the I/O-bound and CPU-bound benchmarks. Runs without Spring or a
 * database.
 *
 * <pre>
 * mvn -q compile
 * java -cp target/classes com.mb.modules.virtualthread.benchmark.VirtualThreadBenchmarkApp \
 *     mode=all tasks=1000,5000,10000 ioWaitMs=100 poolSize=100
 * </pre>
 *
 * <p>Options (all {@code key=value}, all optional): {@code mode} (io | cpu | all), {@code tasks},
 * {@code ioWaitMs}, {@code poolSize}, {@code cpuTasks}, {@code primeLimit}, {@code cpuPoolSize},
 * {@code warmups}, {@code runs}.
 *
 * @author pravin.sahu
 */
public final class VirtualThreadBenchmarkApp {

  private static final Set<String> KNOWN_OPTIONS =
      Set.of(
          "mode",
          "tasks",
          "ioWaitMs",
          "poolSize",
          "cpuTasks",
          "primeLimit",
          "cpuPoolSize",
          "warmups",
          "runs");

  private static final String DEFAULT_IO_TASKS = "1000,5000,10000";
  private static final String DEFAULT_IO_WAIT_MS = "100";
  private static final String DEFAULT_IO_POOL_SIZE = "100";
  private static final String DEFAULT_PRIME_LIMIT = "2000000";
  private static final String DEFAULT_WARMUPS = "1";
  private static final String DEFAULT_RUNS = "3";

  private VirtualThreadBenchmarkApp() {}

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    run(args, System.out);
  }

  public static void run(String[] args, PrintStream out)
      throws InterruptedException, ExecutionException {
    Map<String, String> options = parse(args);
    BenchmarkRunner runner = new BenchmarkRunner();
    int warmups = intOption(options, "warmups", DEFAULT_WARMUPS);
    int runs = intOption(options, "runs", DEFAULT_RUNS);

    String mode = options.getOrDefault("mode", "all");
    switch (mode) {
      case "io" -> runIo(runner, options, warmups, runs, out);
      case "cpu" -> runCpu(runner, options, warmups, runs, out);
      case "all" -> {
        runIo(runner, options, warmups, runs, out);
        runCpu(runner, options, warmups, runs, out);
      }
      default -> throw new IllegalArgumentException("Unknown mode: " + mode);
    }
  }

  private static void runIo(
      BenchmarkRunner runner, Map<String, String> options, int warmups, int runs, PrintStream out)
      throws InterruptedException, ExecutionException {

    long ioWaitMs = intOption(options, "ioWaitMs", DEFAULT_IO_WAIT_MS);
    int poolSize = intOption(options, "poolSize", DEFAULT_IO_POOL_SIZE);
    List<Integer> taskCounts =
        Arrays.stream(options.getOrDefault("tasks", DEFAULT_IO_TASKS).split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .toList();

    for (int taskCount : taskCounts) {
      out.print(BenchmarkReportFormatter.ioHeader(taskCount, ioWaitMs, poolSize, warmups, runs));
      out.flush();
      BenchmarkRuns platform =
          runner.runRepeated(
              ThreadMode.PLATFORM,
              taskCount,
              poolSize,
              () -> Workloads.simulateIoWait(ioWaitMs),
              warmups,
              runs);
      out.print(BenchmarkReportFormatter.section("Platform Threads", platform));
      out.flush();
      BenchmarkRuns virtual =
          runner.runRepeated(
              ThreadMode.VIRTUAL,
              taskCount,
              poolSize,
              () -> Workloads.simulateIoWait(ioWaitMs),
              warmups,
              runs);
      out.print(BenchmarkReportFormatter.section("Virtual Threads", virtual));
      out.print(BenchmarkReportFormatter.comparison(platform, virtual));
      out.println();
      out.flush();
    }
  }

  private static void runCpu(
      BenchmarkRunner runner, Map<String, String> options, int warmups, int runs, PrintStream out)
      throws InterruptedException, ExecutionException {

    int processors = Runtime.getRuntime().availableProcessors();
    int taskCount = intOption(options, "cpuTasks", String.valueOf(processors * 2));
    int primeLimit = intOption(options, "primeLimit", DEFAULT_PRIME_LIMIT);
    int poolSize = intOption(options, "cpuPoolSize", String.valueOf(processors));

    out.print(BenchmarkReportFormatter.cpuHeader(taskCount, primeLimit, processors, warmups, runs));
    out.flush();
    BenchmarkRuns platform =
        runner.runRepeated(
            ThreadMode.PLATFORM,
            taskCount,
            poolSize,
            () -> Workloads.countPrimes(primeLimit),
            warmups,
            runs);
    out.print(BenchmarkReportFormatter.section("Platform Threads", platform));
    out.flush();
    BenchmarkRuns virtual =
        runner.runRepeated(
            ThreadMode.VIRTUAL,
            taskCount,
            poolSize,
            () -> Workloads.countPrimes(primeLimit),
            warmups,
            runs);
    out.print(BenchmarkReportFormatter.section("Virtual Threads", virtual));
    out.print(BenchmarkReportFormatter.comparison(platform, virtual));
    out.print(BenchmarkReportFormatter.cpuNote(processors));
    out.println();
    out.flush();
  }

  private static Map<String, String> parse(String[] args) {
    Map<String, String> options = new HashMap<>();
    for (String arg : args) {
      String[] keyValue = arg.split("=", 2);
      if (keyValue.length != 2 || !KNOWN_OPTIONS.contains(keyValue[0])) {
        throw new IllegalArgumentException(
            "Invalid option '" + arg + "'. Expected key=value with key in " + KNOWN_OPTIONS);
      }
      options.put(keyValue[0], keyValue[1]);
    }
    return options;
  }

  private static int intOption(Map<String, String> options, String key, String defaultValue) {
    int value = Integer.parseInt(options.getOrDefault(key, defaultValue));
    if (value < 0) {
      throw new IllegalArgumentException(key + " must not be negative");
    }
    return value;
  }
}
