package com.mb.modules.virtualthread.integration.controller;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.COMPARE_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.INFO_URL;
import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.IO_URL;
import static org.assertj.core.api.Assertions.assertThat;

import com.mb.base.AbstractBaseIntegrationTest;
import com.mb.modules.virtualthread.config.VirtualThreadPocConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Full-stack integration tests for {@link
 * com.mb.modules.virtualthread.controller.VirtualThreadController}.
 *
 * <p>{@code @ActiveProfiles} adds the PoC profile to the inherited {@code test} profile, because
 * the PoC beans only exist in that profile.
 *
 * <p>MockMvc executes requests on the calling test thread, not on Tomcat request threads, so these
 * tests verify wiring (security rule, controller → service → runner) rather than whether Tomcat
 * uses virtual threads. That is verified by running the application with the {@code
 * virtual-thread-poc} profile (see {@code docs/virtual-threads-poc.md}).
 *
 * @author pravin.sahu
 */
@AutoConfigureRestTestClient
@ActiveProfiles(VirtualThreadPocConfig.PROFILE)
@DisplayName("VirtualThreadController integration – /v1/virtual-threads/** full-stack")
class VirtualThreadControllerIntegrationTest extends AbstractBaseIntegrationTest {

  @Autowired private RestTestClient restClient;

  @Test
  @DisplayName("info is reachable without authentication")
  void infoWithoutAuthenticationReturnsOk() {
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
        .jsonPath("$.data.threadName")
        .exists();
  }

  @Test
  @DisplayName("io waits for the requested delay")
  void ioReturnsElapsedTime() {
    restClient
        .get()
        .uri(IO_URL + "?delayMs=10")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.requestedDelayMs")
        .isEqualTo(10);
  }

  @Test
  @DisplayName("IO comparison: platform is bounded by the pool, virtual runs every task at once")
  void compareIoRunsBothModes() {
    restClient
        .get()
        .uri(COMPARE_URL + "?workload=IO&tasks=50&delayMs=10&poolSize=5")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.sameWorkVerified")
        .isEqualTo(true)
        .jsonPath("$.data.platform.tasksOnVirtualThreads")
        .isEqualTo(0)
        .jsonPath("$.data.platform.maxObservedConcurrency")
        .value(Integer.class, value -> assertThat(value).isLessThanOrEqualTo(5))
        .jsonPath("$.data.virtual.tasksOnVirtualThreads")
        .isEqualTo(50)
        .jsonPath("$.data.virtual.maxObservedConcurrency")
        .isEqualTo(50)
        .jsonPath("$.data.virtual.sampleThread")
        .value(String.class, value -> assertThat(value).startsWith("VirtualThread["));
  }

  @Test
  @DisplayName("CPU comparison: virtual concurrency never exceeds the core count")
  void compareCpuIsBoundedByCores() {
    int processors = Runtime.getRuntime().availableProcessors();

    restClient
        .get()
        .uri(COMPARE_URL + "?workload=CPU&tasks=8&primeLimit=20000")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.workload")
        .isEqualTo("CPU")
        .jsonPath("$.data.sameWorkVerified")
        .isEqualTo(true)
        .jsonPath("$.data.virtual.maxObservedConcurrency")
        .value(Integer.class, value -> assertThat(value).isLessThanOrEqualTo(processors));
  }

  @Test
  @DisplayName("too-long IO run → 400 INVALID_REQUEST")
  void compareTooLongRunReturnsBadRequest() {
    restClient
        .get()
        .uri(COMPARE_URL + "?workload=IO&tasks=5000&delayMs=2000&poolSize=1")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.errorCode")
        .isEqualTo("INVALID_REQUEST");
  }
}
