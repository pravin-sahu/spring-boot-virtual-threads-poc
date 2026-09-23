package com.mb.modules.virtualthread.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mb.modules.virtualthread.enums.WorkloadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * The whole PoC in one response: the same workload run on platform threads and on virtual threads.
 *
 * @author pravin.sahu
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
public class ComparisonResponseDto {

  private WorkloadType workload;

  private int tasks;

  private int runs;

  /** Simulated wait per task; only present for {@link WorkloadType#IO}. */
  private Long delayMs;

  /** Prime limit per task; only present for {@link WorkloadType#CPU}. */
  private Integer primeLimit;

  private ModeResultResponseDto platform;

  private ModeResultResponseDto virtual;

  /** Platform time divided by virtual time. Around 1.0 means no difference. */
  private double speedup;

  /** True when both modes produced the same checksum, i.e. they really did the same work. */
  private boolean sameWorkVerified;

  private String summary;
}
