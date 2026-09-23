package com.mb.modules.virtualthread.unit.controller;

import static com.mb.modules.virtualthread.testdata.VirtualThreadTestDataBuilder.INFO_URL;

import com.mb.base.MockMvcSecurityConfig;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.infrastructure.security.config.CustomAuthenticationEntryPoint;
import com.mb.infrastructure.security.config.SecurityConfig;
import com.mb.modules.virtualthread.controller.VirtualThreadController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Verifies that the PoC endpoints do not exist unless the {@code virtual-thread-poc} profile is
 * active, so the PoC-only {@code permitAll} rule cannot expose anything in other environments.
 *
 * @author pravin.sahu
 */
@WebMvcTest(controllers = VirtualThreadController.class)
@AutoConfigureRestTestClient
@Import({
  ApiResponseBuilder.class,
  MockMvcSecurityConfig.class,
  SecurityConfig.class,
  CustomAuthenticationEntryPoint.class
})
@DisplayName("VirtualThreadController – absent without the virtual-thread-poc profile")
class VirtualThreadProfileIsolationTest {

  @Autowired private RestTestClient restClient;

  @Test
  @DisplayName("without the PoC profile → 404, endpoint is not registered")
  void infoWithoutPocProfileReturnsNotFound() {
    restClient.get().uri(INFO_URL).exchange().expectStatus().isNotFound();
  }
}
