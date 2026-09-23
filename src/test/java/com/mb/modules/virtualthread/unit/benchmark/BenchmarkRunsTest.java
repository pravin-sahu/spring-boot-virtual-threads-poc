package com.mb.modules.virtualthread.unit.benchmark;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.TASKS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.mb.modules.virtualthread.benchmark.BenchmarkResult;
import com.mb.modules.virtualthread.benchmark.BenchmarkRuns;
import com.mb.modules.virtualthread.enums.ThreadMode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BenchmarkRuns / BenchmarkResult – aggregate statistics")
class BenchmarkRunsTest {

  @Test
  @DisplayName("computes average, min, max, throughput and peak concurrency")
  void computesAggregates() {
    BenchmarkRuns runs =
        new BenchmarkRuns(
            List.of(
                buildResult(ThreadMode.PLATFORM, 100),
                buildResult(ThreadMode.PLATFORM, 200),
                buildResult(ThreadMode.PLATFORM, 300)));

    assertThat(runs.averageMillis()).isCloseTo(200.0, within(0.001));
    assertThat(runs.minMillis()).isEqualTo(100);
    assertThat(runs.maxMillis()).isEqualTo(300);
    // TASKS / 0.1s, TASKS / 0.2s, TASKS / 0.3s averaged
    double expected = (TASKS / 0.1 + TASKS / 0.2 + TASKS / 0.3) / 3;
    assertThat(runs.averageThroughput()).isCloseTo(expected, within(0.001));
    assertThat(runs.maxConcurrency()).isEqualTo(5);
    assertThat(runs.first().elapsedMillis()).isEqualTo(100);
  }

  @Test
  @DisplayName("throughput is tasks per second")
  void throughputIsTasksPerSecond() {
    BenchmarkResult result = buildResult(ThreadMode.VIRTUAL, 500);

    assertThat(result.throughputPerSecond()).isCloseTo(TASKS / 0.5, within(0.001));
  }

  @Test
  @DisplayName("rejects an empty list of runs")
  void rejectsEmptyRuns() {
    assertThatThrownBy(() -> new BenchmarkRuns(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
