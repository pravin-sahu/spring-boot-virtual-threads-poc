package com.mb.modules.user.unit.dao;

import static com.mb.modules.user.testdata.UserTestDataBuilder.EMAIL;
import static com.mb.modules.user.testdata.UserTestDataBuilder.VALID_UUID;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUser;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUserResponseDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.modules.user.dao.UserDaoImpl;
import com.mb.modules.user.dto.response.UserResponseDto;
import com.mb.modules.user.entity.User;
import com.mb.modules.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link UserDaoImpl}.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDaoImpl – user persistence operations")
class UserDaoImplTest {

  @Mock private UserRepository userRepo;

  @InjectMocks private UserDaoImpl userDao;

  // -------------------------------------------------------------------------
  // saveUser
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("saveUser: valid user → saved entity returned and repository.save called once")
  void saveUserWithValidUserReturnsSavedUser() {
    User user = buildUser();
    when(userRepo.save(user)).thenReturn(user);

    User result = userDao.saveUser(user);

    assertThat(result).isEqualTo(user);
    verify(userRepo).save(user);
  }

  @Test
  @DisplayName(
      "saveUser: repository throws RuntimeException → wrapped in AppException with 500 status")
  void saveUserWhenRepThrowsRuntimeExceptionWrapsInAppException() {
    User user = buildUser();
    when(userRepo.save(user)).thenThrow(new RuntimeException("constraint violation"));

    assertThatThrownBy(() -> userDao.saveUser(user))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).isEqualTo(ExceptionMessage.INTERNAL_SERVER_ERROR);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              assertThat(ae.getDetail()).isEqualTo("constraint violation");
            });
  }

  @Test
  @DisplayName(
      "saveUser: DataIntegrityViolationException (e.g. unique constraint) → wrapped in AppException"
          + " with 409 CONFLICT status")
  void saveUserWhenDataIntegrityViolationExceptionWrapsInAppException() {
    User user = buildUser();
    when(userRepo.save(user))
        .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

    assertThatThrownBy(() -> userDao.saveUser(user))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).isEqualTo(ExceptionMessage.DUPLICATE_RESOURCE);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(ae.getDetail()).contains("unique constraint violated");
            });
  }

  // -------------------------------------------------------------------------
  // userByEmail
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "userByEmail: existing email → projection returned and repository called with correct args")
  void userByEmailWhenUserExistsReturnsProjection() {
    UserResponseDto dto = buildUserResponseDto();
    when(userRepo.findByEmail(EMAIL, UserResponseDto.class)).thenReturn(Optional.of(dto));

    UserResponseDto result = userDao.userByEmail(EMAIL, UserResponseDto.class);

    assertThat(result).isEqualTo(dto);
    assertThat(result.getEmail()).isEqualTo(EMAIL);
    verify(userRepo).findByEmail(EMAIL, UserResponseDto.class);
  }

  @Test
  @DisplayName("userByEmail: unknown email → AppException with 404 NOT_FOUND status")
  void userByEmailWhenUserNotFoundThrowsAppExceptionWithNotFound() {
    when(userRepo.findByEmail(anyString(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDao.userByEmail(EMAIL, UserResponseDto.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).isEqualTo(ExceptionMessage.USER_NOT_FOUND);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName(
      "userByEmail: repository throws unexpected RuntimeException → propagates unwrapped (no"
          + " try-catch in DAO)")
  void userByEmailWhenRepositoryThrowsRuntimeExceptionPropagatesUnwrapped() {
    RuntimeException dbException = new RuntimeException("Database connection lost");
    when(userRepo.findByEmail(EMAIL, UserResponseDto.class)).thenThrow(dbException);

    assertThatThrownBy(() -> userDao.userByEmail(EMAIL, UserResponseDto.class))
        .isInstanceOf(RuntimeException.class)
        .isNotInstanceOf(AppException.class)
        .hasMessage("Database connection lost");
  }

  // -------------------------------------------------------------------------
  // userByUuid
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("userByUuid: existing UUID → projection returned with correct UUID value")
  void userByUuidWhenUserExistsReturnsProjection() {
    UserResponseDto dto = buildUserResponseDto();
    when(userRepo.findByUuid(VALID_UUID, UserResponseDto.class)).thenReturn(Optional.of(dto));

    UserResponseDto result = userDao.userByUuid(VALID_UUID, UserResponseDto.class);

    assertThat(result).isEqualTo(dto);
    assertThat(result.getUuid()).isEqualTo(VALID_UUID);
  }

  @Test
  @DisplayName("userByUuid: unknown UUID → AppException message contains the UUID string")
  void userByUuidWhenUserNotFoundThrowsAppExceptionContainingUuid() {
    when(userRepo.findByUuid(VALID_UUID, UserResponseDto.class)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDao.userByUuid(VALID_UUID, UserResponseDto.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage())
                  .startsWith(ExceptionMessage.USER_NOT_FOUND_BY_UUID)
                  .contains(VALID_UUID.toString());
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName(
      "userByUuid: repository throws unexpected RuntimeException → propagates unwrapped (no"
          + " try-catch in DAO)")
  void userByUuidWhenRepositoryThrowsRuntimeExceptionPropagatesUnwrapped() {
    RuntimeException dbException = new RuntimeException("Database connection lost");
    when(userRepo.findByUuid(VALID_UUID, UserResponseDto.class)).thenThrow(dbException);

    assertThatThrownBy(() -> userDao.userByUuid(VALID_UUID, UserResponseDto.class))
        .isInstanceOf(RuntimeException.class)
        .isNotInstanceOf(AppException.class)
        .hasMessage("Database connection lost");
  }
}
