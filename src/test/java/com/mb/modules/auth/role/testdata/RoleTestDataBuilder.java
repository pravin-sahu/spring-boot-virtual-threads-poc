package com.mb.modules.auth.role.testdata;

import com.mb.modules.auth.role.entity.Role;
import java.util.UUID;

/**
 * Central test-data factory for role-related tests.
 *
 * @author rohit.kavthekar
 */
public class RoleTestDataBuilder {

  public static final String ADMIN_ROLE = "ADMIN";
  public static final String USER_ROLE = "USER";
  public static final String MODERATOR_ROLE = "MODERATOR";
  public static final String NONEXISTENT_ROLE = "NONEXISTENT_ROLE";

  private RoleTestDataBuilder() {}

  /** Returns a {@link Role} with a random UUID and the supplied {@code name}. */
  public static Role buildRole(String name) {
    Role role = new Role();
    role.setUuid(UUID.randomUUID());
    role.setName(name);
    role.setDescription(name + " role");
    return role;
  }

  /** Convenience method – returns a {@link Role} pre-configured as {@link #ADMIN_ROLE}. */
  public static Role buildAdminRole() {
    return buildRole(ADMIN_ROLE);
  }

  /** Convenience method – returns a {@link Role} pre-configured as {@link #USER_ROLE}. */
  public static Role buildUserRole() {
    return buildRole(USER_ROLE);
  }

  /** Convenience method – returns a {@link Role} pre-configured as {@link #MODERATOR_ROLE}. */
  public static Role buildModeratorRole() {
    return buildRole(MODERATOR_ROLE);
  }
}
