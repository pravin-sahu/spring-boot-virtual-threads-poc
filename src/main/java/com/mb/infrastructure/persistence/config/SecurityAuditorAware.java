package com.mb.infrastructure.persistence.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides the current authenticated user's identifier to JPA auditing. Populates the {@code
 * created_by} and {@code updated_by} columns in {@link
 * com.mb.infrastructure.persistence.entity.BaseEntity}.
 *
 * @author rohit.kavthekar
 */
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

  private static final String SYSTEM_USER = "system";
  private static final String ANONYMOUS_USER = "anonymousUser";

  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      return Optional.of(SYSTEM_USER); // fallback for background jobs / migrations
    }

    String principal = auth.getName();

    if (ANONYMOUS_USER.equals(principal)) {
      return Optional.of(SYSTEM_USER);
    }

    return Optional.of(principal);
  }
}
