package com.mb.infrastructure.persistence.config;

import static com.mb.modules.user.testdata.UserTestDataBuilder.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link SecurityAuditorAware}.
 *
 * <p>Verifies the {@code getCurrentAuditor()} method under all meaningful security context states:
 * authenticated user, anonymous user, null authentication, and unauthenticated token. Ensures the
 * {@code "system"} fallback is applied whenever no real principal is available.
 *
 * @author rohit.kavthekar
 */
@DisplayName("SecurityAuditorAware – current auditor resolution from security context")
class SecurityAuditorAwareTest {

  private static final String SYSTEM_USER = "system";
  private static final String SERVICE_ACCOUNT = "service-account@internal.com";

  private SecurityAuditorAware auditorAware;

  @BeforeEach
  void setUp() {
    auditorAware = new SecurityAuditorAware();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  // -------------------------------------------------------------------------
  // Authenticated users
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("authenticated user → returns the principal name from the authentication token")
  void getCurrentAuditorWithAuthenticatedUserReturnsUsername() {
    Authentication auth =
        UsernamePasswordAuthenticationToken.authenticated(
            EMAIL, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).contains(EMAIL);
  }

  @Test
  @DisplayName("service-account principal → returns the service-account name")
  void getCurrentAuditorWithServiceAccountReturnsItsName() {
    Authentication auth =
        UsernamePasswordAuthenticationToken.authenticated(
            SERVICE_ACCOUNT, "secret", List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).contains(SERVICE_ACCOUNT);
  }

  // -------------------------------------------------------------------------
  // System fallback
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("no authentication in context → returns 'system'")
  void getCurrentAuditorWithNoAuthenticationReturnsSystem() {
    SecurityContextHolder.clearContext();

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).contains(SYSTEM_USER);
  }

  @Test
  @DisplayName("anonymous user principal → returns 'system'")
  void getCurrentAuditorWithAnonymousUserReturnsSystem() {
    Authentication auth =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).contains(SYSTEM_USER);
  }

  @Test
  @DisplayName("unauthenticated token (isAuthenticated=false) → returns 'system'")
  void getCurrentAuditorWithUnauthenticatedTokenReturnsSystem() {
    Authentication auth = UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "password");
    SecurityContextHolder.getContext().setAuthentication(auth);

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).contains(SYSTEM_USER);
  }

  // -------------------------------------------------------------------------
  // Return type guarantee
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("result is always a non-empty Optional regardless of auth state")
  void getCurrentAuditorAlwaysReturnsNonEmptyOptional() {
    SecurityContextHolder.clearContext();

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).isPresent();
  }
}
