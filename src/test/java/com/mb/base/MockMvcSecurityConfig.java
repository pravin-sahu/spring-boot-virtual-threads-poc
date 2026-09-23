package com.mb.base;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Applies {@code springSecurity()} to the auto-configured MockMvc so that Spring Boot 4.x's {@code
 * MockMvcConfiguration} routes test requests through the real Spring Security filter chain. Without
 * this customizer, {@code @WebMvcTest} does not wire security into MockMvc and unauthenticated
 * requests reach the controller instead of being rejected with 401.
 *
 * <p>Import this class in every {@code @WebMvcTest} that needs to verify security behaviour:
 *
 * <pre>{@code
 * @Import({ApiResponseBuilder.class, MockMvcSecurityConfig.class})
 * }</pre>
 *
 * @author rohit.kavthekar
 */
@TestConfiguration
public class MockMvcSecurityConfig {

  @Bean
  MockMvcBuilderCustomizer securityCustomizer() {
    return builder -> builder.apply(springSecurity());
  }
}
