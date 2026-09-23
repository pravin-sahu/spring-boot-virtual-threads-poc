package com.mb.modules.auth.identity.integration.repository;

import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.AUTH0_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.LOCAL_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.NONEXISTENT_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.buildIdentity;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mb.base.AbstractBaseJpaTest;
import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import com.mb.modules.auth.identity.repository.UserIdentityRepository;
import com.mb.modules.user.entity.User;
import com.mb.modules.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Integration tests for {@link UserIdentityRepository} against a real PostgreSQL database managed
 * by Testcontainers. Uses {@code @DataJpaTest} so only the JPA slice is loaded.
 *
 * @author rohit.kavthekar
 */
@DisplayName("UserIdentityRepository – JPA queries against real database")
class UserIdentityRepositoryIntegrationTest extends AbstractBaseJpaTest {

  @Autowired private UserIdentityRepository identityRepo;
  @Autowired private UserRepository userRepo;

  private User savedUser;

  @BeforeEach
  void setUp() {
    savedUser = userRepo.save(buildUser("identity.test@example.com"));
  }

  // -------------------------------------------------------------------------
  // findByProviderAndProviderUserId
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("findByProviderAndProviderUserId: matching provider + id → Optional present")
  void findByProviderAndProviderUserIdWithExistingIdentityReturnsIdentity() {
    identityRepo.save(buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID));

    Optional<UserIdentity> result =
        identityRepo.findByProviderAndProviderUserId(
            AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(result).isPresent();
    assertThat(result.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(result.get().getProviderUserId()).isEqualTo(LOCAL_PROVIDER_USER_ID);
  }

  @Test
  @DisplayName("findByProviderAndProviderUserId: wrong provider for known id → empty Optional")
  void findByProviderAndProviderUserIdWithWrongProviderReturnsEmpty() {
    identityRepo.save(buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID));

    Optional<UserIdentity> result =
        identityRepo.findByProviderAndProviderUserId(
            AuthProvider.AUTH0, LOCAL_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByProviderAndProviderUserId: non-existent providerUserId → empty Optional")
  void findByProviderAndProviderUserIdWithNonExistentIdReturnsEmpty() {
    Optional<UserIdentity> result =
        identityRepo.findByProviderAndProviderUserId(
            AuthProvider.AUTH0, NONEXISTENT_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByProviderAndProviderUserId: multiple identities for user → returns by key")
  void findByProviderAndProviderUserIdWithMultipleIdentitiesReturnsByKey() {
    identityRepo.save(buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID));
    identityRepo.save(buildIdentity(savedUser, AuthProvider.AUTH0, AUTH0_PROVIDER_USER_ID));

    Optional<UserIdentity> local =
        identityRepo.findByProviderAndProviderUserId(
            AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class);
    Optional<UserIdentity> auth0 =
        identityRepo.findByProviderAndProviderUserId(
            AuthProvider.AUTH0, AUTH0_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(local).isPresent();
    assertThat(local.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(auth0).isPresent();
    assertThat(auth0.get().getProvider()).isEqualTo(AuthProvider.AUTH0);
  }

  // -------------------------------------------------------------------------
  // Unique constraint – (provider, provider_user_id)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("save duplicate (provider, providerUserId) → DataIntegrityViolationException")
  void saveDuplicateProviderKeyThrowsDataIntegrityViolationException() {
    identityRepo.save(buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID));
    identityRepo.flush();

    assertThatThrownBy(
            () ->
                identityRepo.saveAndFlush(
                    buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // -------------------------------------------------------------------------
  // save – audit fields
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("save: id, uuid, createdAt, updatedAt populated after persist")
  void savePersistsIdentityAndPopulatesAuditFields() {
    UserIdentity saved =
        identityRepo.save(buildIdentity(savedUser, AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUuid()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getProvider()).isEqualTo(AuthProvider.LOCAL);
  }
}
