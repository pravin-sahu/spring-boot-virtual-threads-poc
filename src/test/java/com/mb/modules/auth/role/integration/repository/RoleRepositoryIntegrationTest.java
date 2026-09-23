package com.mb.modules.auth.role.integration.repository;

import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.NONEXISTENT_ROLE;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.buildAdminRole;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.buildUserRole;
import static org.assertj.core.api.Assertions.assertThat;

import com.mb.base.AbstractBaseJpaTest;
import com.mb.modules.auth.role.entity.Role;
import com.mb.modules.auth.role.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link RoleRepository} against a real PostgreSQL database managed by
 * Testcontainers. Uses {@code @DataJpaTest} so only the JPA slice is loaded.
 *
 * @author rohit.kavthekar
 */
@DisplayName("RoleRepository – JPA queries against real database")
class RoleRepositoryIntegrationTest extends AbstractBaseJpaTest {

  @Autowired private RoleRepository roleRepo;

  @AfterEach
  void tearDown() {
    roleRepo.deleteAll();
  }

  // -------------------------------------------------------------------------
  // findByName
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("findByName: existing role → Optional present with correct name")
  void findByNameWithExistingRoleReturnsRole() {
    roleRepo.save(buildAdminRole());

    Optional<Role> result = roleRepo.findByName("ADMIN", Role.class);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("findByName: non-existent name → empty Optional")
  void findByNameWithNonExistentNameReturnsEmpty() {
    Optional<Role> result = roleRepo.findByName(NONEXISTENT_ROLE, Role.class);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findByName: multiple roles saved → returns correct role by name")
  void findByNameWithMultipleRolesReturnsByName() {
    roleRepo.save(buildAdminRole());
    roleRepo.save(buildUserRole());

    Optional<Role> admin = roleRepo.findByName("ADMIN", Role.class);
    Optional<Role> user = roleRepo.findByName("USER", Role.class);

    assertThat(admin).isPresent();
    assertThat(admin.get().getName()).isEqualTo("ADMIN");
    assertThat(user).isPresent();
    assertThat(user.get().getName()).isEqualTo("USER");
  }

  // -------------------------------------------------------------------------
  // save – audit fields
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("save: id, uuid, createdAt, updatedAt populated after persist")
  void savePersistsRoleAndPopulatesAuditFields() {
    Role saved = roleRepo.save(buildAdminRole());

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUuid()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getName()).isEqualTo("ADMIN");
  }
}
