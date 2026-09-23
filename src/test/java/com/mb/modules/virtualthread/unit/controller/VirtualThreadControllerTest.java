package com.mb.modules.virtualthread.unit.controller;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.CONCURRENT_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.INFO_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.IO_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.POOL_SIZE;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.TASKS;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.VIRTUAL_THREAD_NAME;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildConcurrentRun;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildIoSimulation;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.buildVirtualThreadInfo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.mb.modules.virtualthread.enums.ThreadMode;
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
        .jsonPath("$.success")
        .isEqualTo(true)
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
  // GET /v1/virtual-threads/concurrent
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("concurrent with explicit params → 200 with batch measurements")
  void concurrentWithParamsReturnsOk() {
    when(virtualThreadService.runConcurrentIo(ThreadMode.PLATFORM, TASKS, 50, POOL_SIZE))
        .thenReturn(buildConcurrentRun(ThreadMode.PLATFORM));

    restClient
        .get()
        .uri(CONCURRENT_URL + "?mode=PLATFORM&tasks={t}&delayMs=50&poolSize={p}", TASKS, POOL_SIZE)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.mode")
        .isEqualTo("PLATFORM")
        .jsonPath("$.data.tasks")
        .isEqualTo(TASKS)
        .jsonPath("$.data.poolSize")
        .isEqualTo(POOL_SIZE);
  }

  @Test
  @DisplayName("concurrent without params → defaults VIRTUAL, 1000 tasks, 100 ms, pool 100")
  void concurrentUsesDefaults() {
    when(virtualThreadService.runConcurrentIo(ThreadMode.VIRTUAL, 1000, 100, 100))
        .thenReturn(buildConcurrentRun(ThreadMode.VIRTUAL));

    restClient
        .get()
        .uri(CONCURRENT_URL)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.poolSize")
        .doesNotExist();
  }

  @Test
  @DisplayName("concurrent with unknown mode or tasks out of range → 400")
  void concurrentWithInvalidParamsReturnsBadRequest() {
    restClient.get().uri(CONCURRENT_URL + "?mode=GPU").exchange().expectStatus().isBadRequest();
    restClient.get().uri(CONCURRENT_URL + "?tasks=0").exchange().expectStatus().isBadRequest();
    restClient.get().uri(CONCURRENT_URL + "?tasks=10001").exchange().expectStatus().isBadRequest();
    restClient.get().uri(CONCURRENT_URL + "?poolSize=0").exchange().expectStatus().isBadRequest();
  }

  @Test
  @DisplayName("service rejects too-long platform run → 400 with INVALID_REQUEST")
  void concurrentWhenServiceRejectsReturnsBadRequest() {
    when(virtualThreadService.runConcurrentIo(eq(ThreadMode.PLATFORM), anyInt(), anyLong(), eq(1)))
        .thenThrow(
            new AppException(
                ExceptionMessage.VIRTUAL_THREAD_RUN_TOO_LONG,
                null,
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST));

    restClient
        .get()
        .uri(CONCURRENT_URL + "?mode=PLATFORM&tasks=10000&delayMs=1000&poolSize=1")
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
