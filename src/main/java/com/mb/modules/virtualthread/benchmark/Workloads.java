package com.mb.modules.virtualthread.benchmark;

/**
 * Deterministic workloads used by the benchmarks and the REST demo.
 *
 * <p>Every workload returns a value so that callers can build a checksum and prove that both thread
 * modes executed exactly the same work.
 *
 * @author pravin.sahu
 */
public final class Workloads {

  private Workloads() {}

  /**
   * Simulates waiting for an external resource by sleeping.
   *
   * <p>This is only a simulation of an I/O <em>wait</em>. No database, network or disk is touched.
   * It is useful because a sleeping virtual thread parks and unmounts from its carrier, similar to
   * a virtual thread blocked on a socket read, but it says nothing about real driver, pool or
   * server limits.
   *
   * @param waitMillis how long to wait
   * @return always {@code 1}, so a batch checksum equals the number of completed tasks
   * @throws InterruptedException if the waiting thread is interrupted
   */
  public static long simulateIoWait(long waitMillis) throws InterruptedException {
    Thread.sleep(waitMillis);
    return 1L;
  }

  /**
   * CPU-bound work: counts primes in {@code [2, limit]} by trial division. The result is fully
   * deterministic, and the thread never blocks, so it never gives up its carrier voluntarily.
   *
   * @param limit inclusive upper bound
   * @return number of primes up to {@code limit}
   */
  public static long countPrimes(int limit) {
    long count = 0;
    for (int candidate = 2; candidate <= limit; candidate++) {
      if (isPrime(candidate)) {
        count++;
      }
    }
    return count;
  }

  private static boolean isPrime(int candidate) {
    for (int divisor = 2; (long) divisor * divisor <= candidate; divisor++) {
      if (candidate % divisor == 0) {
        return false;
      }
    }
    return true;
  }
}
