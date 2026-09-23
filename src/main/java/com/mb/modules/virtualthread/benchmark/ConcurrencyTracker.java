package com.mb.modules.virtualthread.benchmark;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks how many tasks are in flight at the same time and the highest value observed.
 *
 * @author pravin.sahu
 */
public class ConcurrencyTracker {

  private final AtomicInteger inFlight = new AtomicInteger();
  private final AtomicInteger maxObserved = new AtomicInteger();

  public void enter() {
    int current = inFlight.incrementAndGet();
    maxObserved.accumulateAndGet(current, Math::max);
  }

  public void exit() {
    inFlight.decrementAndGet();
  }

  public int maxObserved() {
    return maxObserved.get();
  }
}
