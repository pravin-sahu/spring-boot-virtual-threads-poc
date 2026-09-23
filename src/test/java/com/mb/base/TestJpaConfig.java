package com.mb.base;

import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test-specific JPA configuration.
 *
 * <p>Enables JPA auditing with a {@link AuditorAware} that respects Spring Security context for
 * realistic test scenarios. Works with {@code @WithMockUser} annotations and falls back to "system"
 * when unauthenticated.
 *
 * <p>This mirrors the behavior of the production {@code SecurityAuditorAware} to ensure test
 * assertions match production behavior.
 *
 * @author rohit.kavthekar
 */
@TestConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class TestJpaConfig {

  private static final String SYSTEM_USER = "system";

  @Bean
  public AuditorAware<String> auditorAware() {
    return new AuditorAware<String>() {
      @Override
      public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
          return Optional.of(SYSTEM_USER);
        }

        String principal = auth.getName();

        // "anonymousUser" is Spring Security's default name for unauthenticated requests
        if ("anonymousUser".equals(principal)) {
          return Optional.of(SYSTEM_USER);
        }

        return Optional.of(principal);
      }
    };
  }
}
