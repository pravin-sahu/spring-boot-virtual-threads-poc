package com.mb.modules.virtualthread.unit.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.mb.modules.virtualthread.benchmark.ConcurrencyTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConcurrencyTracker – in-flight and max observed concurrency")
class ConcurrencyTrackerTest {

  @Test
  @DisplayName("max observed keeps the peak even after tasks exit")
  void maxObservedKeepsPeak() {
    ConcurrencyTracker tracker = new ConcurrencyTracker();

    tracker.enter();
    tracker.enter();
    tracker.enter();
    tracker.exit();
    tracker.exit();
    tracker.enter();

    assertThat(tracker.maxObserved()).isEqualTo(3);
  }

  @Test
  @DisplayName("new tracker reports zero")
  void newTrackerReportsZero() {
    assertThat(new ConcurrencyTracker().maxObserved()).isZero();
  }
}
