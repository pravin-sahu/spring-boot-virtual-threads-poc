package com.mb.modules.virtualthread.controller;

import com.mb.common.constant.ApiEndpoint;
import com.mb.common.constant.ResponseMessage;
import com.mb.common.dto.response.ApiResponse;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import com.mb.modules.virtualthread.dto.response.ConcurrentRunResponseDto;
import com.mb.modules.virtualthread.dto.response.IoSimulationResponseDto;
import com.mb.modules.virtualthread.dto.response.ThreadInfoResponseDto;
import com.mb.modules.virtualthread.enums.ThreadMode;
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
   * Batch of concurrent simulated I/O waits on platform or virtual threads
   *
   * @param mode thread mode
   * @param tasks number of tasks
   * @param delayMs simulated wait per task in milliseconds
   * @param poolSize platform pool size (ignored for virtual)
   * @return {@link ResponseEntity}
   */
  @GetMapping(value = ApiEndpoint.VIRTUAL_THREADS_CONCURRENT, version = "1.0")
  public ResponseEntity<ApiResponse<ConcurrentRunResponseDto>> runConcurrent(
      @RequestParam(defaultValue = "VIRTUAL") ThreadMode mode,
      @RequestParam(defaultValue = "1000") @Min(1) @Max(10_000) int tasks,
      @RequestParam(defaultValue = "100") @Min(0) @Max(5_000) long delayMs,
      @RequestParam(defaultValue = "100") @Min(1) @Max(1_000) int poolSize) {

    ConcurrentRunResponseDto result =
        virtualThreadService.runConcurrentIo(mode, tasks, delayMs, poolSize);

    return responseBuilder.success(ResponseMessage.SUCCESS, result, HttpStatus.OK);
  }
}
