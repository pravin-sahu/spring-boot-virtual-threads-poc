package com.mb.modules.virtualthread.controller;

import com.mb.common.constant.ApiEndpoint;
import com.mb.common.constant.ResponseMessage;
import com.mb.common.dto.response.ApiResponse;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import com.mb.modules.virtualthread.dto.response.ComparisonResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.WorkloadType;
import com.mb.modules.virtualthread.service.VirtualThreadService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Virtual thread PoC api's controller. Whether requests are served on virtual threads depends on
 * {@code spring.threads.virtual.enabled} (see the {@code virtual-thread-poc} profile).
 *
 * @author pravin.sahu
 */
@RestController
@Profile(VirtualThreadPocConfig.PROFILE)
@RequiredArgsConstructor
@RequestMapping(ApiEndpoint.VIRTUAL_THREADS)
public class VirtualThreadController {

  private final ApiResponseBuilder responseBuilder;

  private final VirtualThreadService virtualThreadService;

  /**
   * Thread handling the current request
   *
   * @return {@link ResponseEntity}
   */
  @GetMapping(value = ApiEndpoint.VIRTUAL_THREADS_INFO, version = "1.0")
  public ResponseEntity<ApiResponse<ThreadInfoResponseDto>> info() {

    ThreadInfoResponseDto threadInfo = virtualThreadService.currentThreadInfo();

    return responseBuilder.success(ResponseMessage.SUCCESS, threadInfo, HttpStatus.OK);
  }

  /**
   * Simulated I/O wait on the request thread
   *
   * @param delayMs simulated wait in milliseconds
   * @return {@link ResponseEntity}
   */
  @GetMapping(value = ApiEndpoint.VIRTUAL_THREADS_IO, version = "1.0")
  public ResponseEntity<ApiResponse<IoSimulationResponseDto>> simulateIo(
      @RequestParam(defaultValue = "200") @Min(0) @Max(10_000) long delayMs) {

    IoSimulationResponseDto result = virtualThreadService.simulateIo(delayMs);

    return responseBuilder.success(ResponseMessage.SUCCESS, result, HttpStatus.OK);
  }

  /**
   * Runs the same workload on platform threads and on virtual threads, and returns both sides
   *
   * @param workload IO (waiting) or CPU (computing)
   * @param tasks number of tasks; defaults to 2000 for IO and 2 x cores for CPU
   * @param delayMs simulated wait per task, used by IO
   * @param primeLimit prime-counting limit per task, used by CPU
   * @param poolSize platform pool size; defaults to 100 for IO and the core count for CPU
   * @param runs how many times to repeat each mode
   * @return {@link ResponseEntity}
   */
  @GetMapping(value = ApiEndpoint.VIRTUAL_THREADS_COMPARE, version = "1.0")
  public ResponseEntity<ApiResponse<ComparisonResponseDto>> compare(
      @RequestParam(defaultValue = "IO") WorkloadType workload,
      @RequestParam(required = false) @Min(1) @Max(5_000) Integer tasks,
      @RequestParam(defaultValue = "100") @Min(0) @Max(2_000) long delayMs,
      @RequestParam(defaultValue = "2000000") @Min(1_000) @Max(3_000_000) int primeLimit,
      @RequestParam(required = false) @Min(1) @Max(500) Integer poolSize,
      @RequestParam(defaultValue = "1") @Min(1) @Max(3) int runs) {

    ComparisonResponseDto result =
        virtualThreadService.compare(workload, tasks, delayMs, primeLimit, poolSize, runs);

    return responseBuilder.success(ResponseMessage.SUCCESS, result, HttpStatus.OK);
  }
}
