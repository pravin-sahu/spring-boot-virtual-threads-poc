package com.mb.modules.virtualthread.enums;

/**
 * Kind of work each task performs during a comparison.
 *
 * @author pravin.sahu
 */
public enum WorkloadType {
  /** Waiting, simulated with {@code Thread.sleep} — the case virtual threads are built for. */
  IO,
  /** Pure computation — the case where virtual threads cannot help. */
  CPU
}
