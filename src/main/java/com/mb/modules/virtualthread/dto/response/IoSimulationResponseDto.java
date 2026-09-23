package com.mb.modules.virtualthread.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Result of a single simulated I/O wait.
 *
 * @author pravin.sahu
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
@AllArgsConstructor
public class IoSimulationResponseDto {

  private long requestedDelayMs;

  private long elapsedMs;

  private ThreadInfoResponseDto thread;
}
