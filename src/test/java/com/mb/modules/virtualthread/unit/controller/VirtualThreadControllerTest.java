package com.mb.modules.virtualthread.unit.controller;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.COMPARE_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.INFO_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.IO_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.POOL_SIZE;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.TASKS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.VIRTUAL_THREAD_NAME;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildComparison;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildIoSimulation;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildVirtualThreadInfo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.base.MockMvcSecurityConfig;
import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.infrastructure.security.config.CustomAuthenticationEntryPoint;
import com.mb.infrastructure.security.config.SecurityConfig;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import com.mb.modules.virtualthread.controller.VirtualThreadController;
import com.mb.modules.virtualthread.enums.WorkloadType;
import com.mb.modules.virtualthread.service.VirtualThreadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Web-layer unit tests for {@link VirtualThreadController}. No test uses {@code @WithMockUser}: the
 * PoC endpoints are deliberately public ({@code permitAll} in {@code SecurityConfig}), so every
 * request here also verifies that rule through the real security filter chain.
 *
 * @author pravin.sahu
 */
@WebMvcTest(controllers = VirtualThreadController.class)
@AutoConfigureRestTestClient
@ActiveProfiles(VirtualThreadPocConfig.PROFILE)
@Import({
  ApiResponseBuilder.class,
  MockMvcSecurityConfig.class,
  SecurityConfig.class,
  CustomAuthenticationEntryPoint.class
})
@DisplayName("VirtualThreadController – /v1/virtual-threads/** web-layer")
class VirtualThreadControllerTest {

  @Autowired private RestTestClient restClient;

  @MockitoBean private VirtualThreadService virtualThreadService;

  // -------------------------------------------------------------------------
  // GET /v1/virtual-threads/info
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("unauthenticated info request → 200 with thread details (PoC endpoint is public)")
  void infoWithoutAuthenticationReturnsOk() {
    when(virtualThreadService.currentThreadInfo()).thenReturn(buildVirtualThreadInfo());

    restClient
        .get()
        .uri(INFO_URL)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.virtual")
        .isEqualTo(true)
        .jsonPath("$.data.threadType")
        .isEqualTo("VIRTUAL")
        .jsonPath("$.data.threadName")
        .isEqualTo(VIRTUAL_THREAD_NAME);
  }

  // -------------------------------------------------------------------------
  // GET /v1/virtual-threads/io
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("io without delayMs → default 200 ms is passed to the service")
  void ioUsesDefaultDelay() {
    when(virtualThreadService.simulateIo(200)).thenReturn(buildIoSimulation());

    restClient
        .get()
        .uri(IO_URL)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.thread.virtual")
        .isEqualTo(true);

    verify(virtualThreadService).simulateIo(200);
  }

  @Test
  @DisplayName("io with delayMs out of range → 400")
  void ioWithDelayOutOfRangeReturnsBadRequest() {
    restClient.get().uri(IO_URL + "?delayMs=-1").exchange().expectStatus().isBadRequest();
    restClient.get().uri(IO_URL + "?delayMs=10001").exchange().expectStatus().isBadRequest();
  }

  // -------------------------------------------------------------------------
  // GET /v1/virtual-threads/compare
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("compare with explicit params → 200 with both modes side by side")
  void compareWithParamsReturnsOk() {
    when(virtualThreadService.compare(WorkloadType.IO, TASKS, 50, 2_000_000, POOL_SIZE, 1))
        .thenReturn(buildComparison(WorkloadType.IO));

    restClient
        .get()
        .uri(COMPARE_URL + "?workload=IO&tasks={t}&delayMs=50&poolSize={p}", TASKS, POOL_SIZE)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.workload")
        .isEqualTo("IO")
        .jsonPath("$.data.platform.threadType")
        .isEqualTo("PLATFORM")
        .jsonPath("$.data.platform.poolSize")
        .isEqualTo(POOL_SIZE)
        .jsonPath("$.data.virtual.threadType")
        .isEqualTo("VIRTUAL")
        .jsonPath("$.data.virtual.poolSize")
        .doesNotExist()
        .jsonPath("$.data.sameWorkVerified")
        .isEqualTo(true)
        .jsonPath("$.data.summary")
        .exists();
  }

  @Test
  @DisplayName("compare without params → IO defaults, tasks and poolSize left to the service")
  void compareUsesDefaults() {
    when(virtualThreadService.compare(
            eq(WorkloadType.IO), isNull(), eq(100L), eq(2_000_000), isNull(), eq(1)))
        .thenReturn(buildComparison(WorkloadType.IO));

    restClient.get().uri(COMPARE_URL).exchange().expectStatus().isOk();

    verify(virtualThreadService).compare(WorkloadType.IO, null, 100, 2_000_000, null, 1);
  }

  @Test
  @DisplayName("compare?workload=CPU → CPU comparison is requested")
  void compareCpuWorkload() {
    when(virtualThreadService.compare(
            eq(WorkloadType.CPU), any(), anyLong(), anyInt(), any(), anyInt()))
        .thenReturn(buildComparison(WorkloadType.CPU));

    restClient
        .get()
        .uri(COMPARE_URL + "?workload=CPU&tasks=8&primeLimit=100000")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.workload")
        .isEqualTo("CPU")
        .jsonPath("$.data.delayMs")
        .doesNotExist();
  }

  @Test
  @DisplayName("compare with invalid parameters → 400")
  void compareWithInvalidParamsReturnsBadRequest() {
    restClient.get().uri(COMPARE_URL + "?workload=GPU").exchange().expectStatus().isBadRequest();
    restClient.get().uri(COMPARE_URL + "?tasks=0").exchange().expectStatus().isBadRequest();
    restClient.get().uri(COMPARE_URL + "?tasks=5001").exchange().expectStatus().isBadRequest();
    restClient.get().uri(COMPARE_URL + "?poolSize=0").exchange().expectStatus().isBadRequest();
    restClient.get().uri(COMPARE_URL + "?runs=4").exchange().expectStatus().isBadRequest();
  }

  @Test
  @DisplayName("service rejects a too-long run → 400 with INVALID_REQUEST")
  void compareWhenServiceRejectsReturnsBadRequest() {
    when(virtualThreadService.compare(any(), any(), anyLong(), anyInt(), any(), anyInt()))
        .thenThrow(
            new AppException(
                ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG,
                null,
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST));

    restClient
        .get()
        .uri(COMPARE_URL + "?tasks=5000&delayMs=2000&poolSize=1")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.errorCode")
        .isEqualTo("INVALID_REQUEST")
        .jsonPath("$.message")
        .isEqualTo(ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG);
  }
}
