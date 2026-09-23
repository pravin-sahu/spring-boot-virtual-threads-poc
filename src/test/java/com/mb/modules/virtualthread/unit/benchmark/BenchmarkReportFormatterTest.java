package com.mb.modules.virtualthread.unit.benchmark;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.mb.modules.virtualthread.benchmark.BenchmarkReportFormatter;
import com.mb.modules.virtualthread.benchmark.BenchmarkResult;
import com.mb.modules.virtualthread.benchmark.BenchmarkRuns;
import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BenchmarkReportFormatter – human-readable report")
class BenchmarkReportFormatterTest {

  @Test
  @DisplayName("I/O header states the simulation and the theoretical minimums")
  void ioHeaderDescribesScenario() {
    String header = BenchmarkReportFormatter.ioHeader(1000, 100, 100, 1, 3);

    assertThat(header)
        .contains("=== I/O-BOUND BENCHMARK ===")
        .contains("Tasks: 1000")
        .contains("Simulated I/O wait: 100 ms")
        .contains("not real I/O")
        .contains("ceil(1000 / 100) x 100 ms = 1000 ms");
  }

  @Test
  @DisplayName("CPU header and note mention the processor count")
  void cpuHeaderAndNoteMentionProcessors() {
    assertThat(BenchmarkReportFormatter.cpuHeader(16, 2_000_000, 8, 1, 3))
        .contains("=== CPU-BOUND BENCHMARK ===")
        .contains("count primes up to 2000000")
        .contains("Available processors: 8");
    assertThat(BenchmarkReportFormatter.cpuNote(8))
        .contains("limited by the 8 available cores")
        .contains("cannot add CPU capacity");
  }

  @Test
  @DisplayName("section shows pool size for platform and n/a for virtual")
  void sectionShowsPoolSize() {
    String platform =
        BenchmarkReportFormatter.section(
            "Platform Threads", new BenchmarkRuns(List.of(buildResult(ThreadMode.PLATFORM, 100))));
    String virtual =
        BenchmarkReportFormatter.section(
            "Virtual Threads", new BenchmarkRuns(List.of(buildResult(ThreadMode.VIRTUAL, 50))));

    assertThat(platform).contains("Platform Threads:").contains("Pool size: 5");
    assertThat(virtual).contains("Pool size: n/a").contains("Tasks run on virtual threads: 20");
  }

  @Test
  @DisplayName("comparison reports ratio and whether checksums match")
  void comparisonReportsRatioAndChecksum() {
    BenchmarkRuns platform = new BenchmarkRuns(List.of(buildResult(ThreadMode.PLATFORM, 200)));
    BenchmarkRuns virtual = new BenchmarkRuns(List.of(buildResult(ThreadMode.VIRTUAL, 100)));
    BenchmarkRuns different =
        new BenchmarkRuns(List.of(new BenchmarkResult(ThreadMode.VIRTUAL, 20, null, 1, 1, 20, 7)));

    assertThat(BenchmarkReportFormatter.comparison(platform, virtual))
        .contains("= 2.00x")
        .contains("Checksums match: yes");
    assertThat(BenchmarkReportFormatter.comparison(platform, different))
        .contains("NO - workloads differ");
  }
}
