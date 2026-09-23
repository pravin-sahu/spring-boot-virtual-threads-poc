package com.mb.modules.auth.role.unit.dao;

import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.ADMIN_ROLE;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.MODERATOR_ROLE;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.NONEXISTENT_ROLE;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.USER_ROLE;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.buildAdminRole;
import static com.mb.modules.auth.role.testdata.RoleTestDataBuilder.buildRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.modules.auth.role.dao.RoleDaoImpl;
import com.mb.modules.auth.role.entity.Role;
import com.mb.modules.auth.role.repository.RoleRepository;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link RoleDaoImpl}.
 *
 * <p>Tests the DAO layer's role retrieval operations, including proper exception handling when
 * roles are not found. Verifies correct delegation to the repository layer and appropriate error
 * message construction with role names.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDaoImpl – role lookup by name")
class RoleDaoImplTest {

  @Mock private RoleRepository roleRepo;

  @InjectMocks private RoleDaoImpl roleDao;

  // -------------------------------------------------------------------------
  // roleByName - Happy path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("existing role name → role entity returned and repository called once")
  void roleByNameWhenRoleExistsReturnsRole() {
    Role role = buildAdminRole();
    when(roleRepo.findByName(ADMIN_ROLE, Role.class)).thenReturn(Optional.of(role));

    Role result = roleDao.roleByName(ADMIN_ROLE, Role.class);

    assertThat(result).isEqualTo(role);
    assertThat(result.getName()).isEqualTo(ADMIN_ROLE);
    verify(roleRepo).findByName(ADMIN_ROLE, Role.class);
  }

  static Stream<String> standardRoleNames() {
    return Stream.of(ADMIN_ROLE, USER_ROLE, MODERATOR_ROLE);
  }

  @ParameterizedTest(name = "role ''{0}'' delegates correctly to repository")
  @MethodSource("standardRoleNames")
  @DisplayName("all standard role names (ADMIN, USER, MODERATOR) delegate correctly")
  void roleByNameWithDifferentRoleNamesDelegatesToRepo(String name) {
    Role role = buildRole(name);
    when(roleRepo.findByName(name, Role.class)).thenReturn(Optional.of(role));

    Role result = roleDao.roleByName(name, Role.class);

    assertThat(result.getName()).isEqualTo(name);
    verify(roleRepo).findByName(name, Role.class);
  }

  // -------------------------------------------------------------------------
  // roleByName - Error handling
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("unknown role name → AppException with 404 status and role name in message")
  void roleByNameWhenRoleNotFoundThrowsAppExceptionContainingName() {
    when(roleRepo.findByName(anyString(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDao.roleByName(NONEXISTENT_ROLE, Role.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage())
                  .startsWith(ExceptionMessage.ROLE_NOT_FOUND)
                  .contains(NONEXISTENT_ROLE);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName("empty string role name → AppException still thrown with 404 status")
  void roleByNameWithEmptyRoleNameThrowsAppException() {
    when(roleRepo.findByName(anyString(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDao.roleByName("", Role.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).startsWith(ExceptionMessage.ROLE_NOT_FOUND);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName("null role name → AppException thrown with 404 status")
  void roleByNameWithNullRoleNameThrowsAppException() {
    when(roleRepo.findByName(null, Role.class)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDao.roleByName(null, Role.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).startsWith(ExceptionMessage.ROLE_NOT_FOUND);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName(
      "repository throws unexpected RuntimeException → exception propagates unwrapped (no try-catch"
          + " in DAO)")
  void roleByNameWhenRepositoryThrowsRuntimeExceptionPropagatesUnwrapped() {
    RuntimeException dbException = new RuntimeException("Database connection lost");
    when(roleRepo.findByName(ADMIN_ROLE, Role.class)).thenThrow(dbException);

    assertThatThrownBy(() -> roleDao.roleByName(ADMIN_ROLE, Role.class))
        .isInstanceOf(RuntimeException.class)
        .isNotInstanceOf(AppException.class)
        .hasMessage("Database connection lost");
  }
}
