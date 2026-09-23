package com.mb.modules.virtualthread.enums;

/**
 * Thread model used to execute a batch of tasks.
 *
 * @author pravin.sahu
 */
public enum ThreadMode {
  /** OS-backed platform threads from a bounded, fixed-size pool. */
  PLATFORM,
  /** One JVM-managed virtual thread per task. */
  VIRTUAL
}
