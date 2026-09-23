package com.mb.modules.virtualthread.unit.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.mb.modules.virtualthread.benchmark.Workloads;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Workloads – simulated I/O wait and deterministic CPU work")
class WorkloadsTest {

  @Test
  @DisplayName("countPrimes returns well-known prime counts")
  void countPrimesReturnsKnownValues() {
    assertThat(Workloads.countPrimes(1)).isZero();
    assertThat(Workloads.countPrimes(10)).isEqualTo(4);
    assertThat(Workloads.countPrimes(100)).isEqualTo(25);
    assertThat(Workloads.countPrimes(10_000)).isEqualTo(1_229);
  }

  @Test
  @DisplayName("simulateIoWait sleeps at least the requested time and returns 1")
  void simulateIoWaitSleepsAndReturnsOne() throws InterruptedException {
    long start = System.nanoTime();

    long result = Workloads.simulateIoWait(20);

    assertThat(result).isEqualTo(1L);
    assertThat(System.nanoTime() - start).isGreaterThanOrEqualTo(20_000_000L);
  }

  @Test
  @DisplayName("simulateIoWait propagates interruption")
  void simulateIoWaitPropagatesInterruption() throws InterruptedException {
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    Thread thread =
        Thread.ofVirtual()
            .start(
                () -> {
                  Thread.currentThread().interrupt();
                  try {
                    Workloads.simulateIoWait(1_000);
                  } catch (InterruptedException e) {
                    thrown.set(e);
                  }
                });
    thread.join();

    assertThat(thrown.get()).isInstanceOf(InterruptedException.class);
  }
}
