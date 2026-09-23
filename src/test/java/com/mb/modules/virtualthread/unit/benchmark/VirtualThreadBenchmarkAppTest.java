package com.mb.modules.virtualthread.unit.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mb.modules.virtualthread.benchmark.VirtualThreadBenchmarkApp;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VirtualThreadBenchmarkApp – command-line entry point with a tiny workload")
class VirtualThreadBenchmarkAppTest {

  private static final String[] TINY =
      new String[] {
        "tasks=4",
        "ioWaitMs=1",
        "poolSize=2",
        "cpuTasks=2",
        "primeLimit=1000",
        "cpuPoolSize=1",
        "warmups=0",
        "runs=1"
      };

  @Test
  @DisplayName("mode=all prints both I/O and CPU reports")
  void allModePrintsBothReports() throws Exception {
    String output = run(append(TINY, "mode=all"));

    assertThat(output)
        .contains("=== I/O-BOUND BENCHMARK ===")
        .contains("=== CPU-BOUND BENCHMARK ===")
        .contains("Platform Threads:")
        .contains("Virtual Threads:")
        .contains("Checksums match: yes");
  }

  @Test
  @DisplayName("mode=io prints only the I/O report, once per task count")
  void ioModePrintsOnlyIoReport() throws Exception {
    String[] args = TINY.clone();
    args[0] = "tasks=2,3";

    String output = run(append(args, "mode=io"));

    assertThat(output).contains("Tasks: 2").contains("Tasks: 3").doesNotContain("CPU-BOUND");
  }

  @Test
  @DisplayName("mode=cpu prints only the CPU report")
  void cpuModePrintsOnlyCpuReport() throws Exception {
    String output = run(append(TINY, "mode=cpu"));

    assertThat(output).contains("CPU-BOUND").doesNotContain("I/O-BOUND");
  }

  @Test
  @DisplayName("invalid arguments are rejected")
  void invalidArgumentsAreRejected() {
    assertThatThrownBy(() -> run(new String[] {"mode=gpu"}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> run(new String[] {"unknown=1"}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> run(new String[] {"noValue"}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> run(new String[] {"runs=-1"}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static String run(String[] args) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    VirtualThreadBenchmarkApp.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8));
    return buffer.toString(StandardCharsets.UTF_8);
  }

  private static String[] append(String[] args, String extra) {
    String[] result = Arrays.copyOf(args, args.length + 1);
    result[args.length] = extra;
    return result;
  }
}
