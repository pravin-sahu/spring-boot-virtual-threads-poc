package com.mb.modules.user.integration.repository;

import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUser;
import static org.assertj.core.api.Assertions.assertThat;

import com.mb.base.AbstractBaseJpaTest;
import com.mb.modules.user.entity.User;
import com.mb.modules.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Integration tests for {@link UserRepository} against a real PostgreSQL database managed by
 * Testcontainers. Uses {@code @DataJpaTest} so only the JPA slice is loaded.
 *
 * @author rohit.kavthekar
 */
@DisplayName("UserRepository – JPA queries and audit field population")
class UserRepositoryIntegrationTest extends AbstractBaseJpaTest {

  @Autowired private UserRepository userRepo;

  @AfterEach
  void tearDown() {
    userRepo.deleteAll();
  }

  // -------------------------------------------------------------------------
  // findByUuid
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("findByUuid: existing UUID → projection returned with matching email and UUID")
  void findByUuidWithExistingUserReturnsUser() {
    User saved = userRepo.save(buildUser("john@example.com"));
    UUID uuid = saved.getUuid();

    Optional<User> result = userRepo.findByUuid(uuid, User.class);

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("john@example.com");
    assertThat(result.get().getUuid()).isEqualTo(uuid);
  }

  @Test
  @DisplayName("findByUuid: non-existent UUID → empty Optional")
  void findByUuidWithNonExistentUuidReturnsEmpty() {
    Optional<User> result = userRepo.findByUuid(UUID.randomUUID(), User.class);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findByEmail
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("findByEmail: existing email → projection returned with matching email")
  void findByEmailWithExistingUserReturnsUser() {
    userRepo.save(buildUser("jane@example.com"));

    Optional<User> result = userRepo.findByEmail("jane@example.com", User.class);

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("jane@example.com");
  }

  @Test
  @DisplayName("findByEmail: non-existent email → empty Optional")
  void findByEmailWithNonExistentEmailReturnsEmpty() {
    Optional<User> result = userRepo.findByEmail("nobody@example.com", User.class);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // save – audit fields
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("save: id, createdAt, and updatedAt are populated after persist")
  void savePersistsUserAndPopulatesAuditFields() {
    User user = buildUser("audit@example.com");

    User saved = userRepo.save(user);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getEmail()).isEqualTo("audit@example.com");
  }

  @Test
  @DisplayName("save: two distinct users → both rows persisted independently")
  void saveWithMultipleUsersPersistsAll() {
    userRepo.save(buildUser("user1@example.com"));
    userRepo.save(buildUser("user2@example.com"));

    assertThat(userRepo.findAll()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  @WithMockUser(username = "audit.test@example.com")
  @DisplayName("save with authenticated user → createdBy and updatedBy set to the principal name")
  void saveShouldPopulateCreatedByWithCurrentUser() {
    User user = buildUser("audit@example.com");

    User saved = userRepo.save(user);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getCreatedBy()).isEqualTo("audit.test@example.com");
    assertThat(saved.getUpdatedBy()).isEqualTo("audit.test@example.com");
  }

  @Test
  @DisplayName("save without authentication → createdBy falls back to 'system'")
  void saveShouldFallbackToSystemUserWhenUnauthenticated() {
    User user = buildUser("noauth@example.com");

    User saved = userRepo.save(user);

    assertThat(saved.getCreatedBy()).isEqualTo("system");
  }
}
