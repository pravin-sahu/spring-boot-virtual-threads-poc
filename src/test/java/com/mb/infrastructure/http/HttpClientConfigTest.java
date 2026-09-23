package com.mb.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for {@link HttpClientConfig}.
 *
 * <p>Verifies that the {@link RestClient} bean is constructed successfully under various timeout
 * configurations. Timeout values are injected via {@link ReflectionTestUtils} to simulate Spring's
 * {@code @Value} injection without loading a full application context.
 *
 * @author rohit.kavthekar
 */
@DisplayName("HttpClientConfig – RestClient bean construction")
class HttpClientConfigTest {

  private HttpClientConfig config;

  @BeforeEach
  void setUp() {
    config = new HttpClientConfig();
    ReflectionTestUtils.setField(config, "connectionTimeout", 5000);
    ReflectionTestUtils.setField(config, "readTimeout", 10000);
  }

  @Test
  @DisplayName("restClient bean is non-null with default timeouts")
  void restClientBeanIsNonNullWithDefaultTimeouts() {
    RestClient restClient = config.restClient();

    assertThat(restClient).isNotNull();
  }

  @Test
  @DisplayName("restClient bean is created with custom timeout values")
  void restClientBeanIsCreatedWithCustomTimeouts() {
    ReflectionTestUtils.setField(config, "connectionTimeout", 1000);
    ReflectionTestUtils.setField(config, "readTimeout", 3000);

    assertThatNoException().isThrownBy(() -> config.restClient());
  }

  @Test
  @DisplayName("restClient bean is created with minimum viable timeouts")
  void restClientBeanIsCreatedWithMinimumTimeouts() {
    ReflectionTestUtils.setField(config, "connectionTimeout", 1);
    ReflectionTestUtils.setField(config, "readTimeout", 1);

    RestClient restClient = config.restClient();

    assertThat(restClient).isNotNull();
  }
}
