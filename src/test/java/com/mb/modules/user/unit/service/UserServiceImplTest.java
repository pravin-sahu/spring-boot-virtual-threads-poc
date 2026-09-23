package com.mb.modules.user.unit.service;

import static com.mb.modules.user.testdata.UserTestDataBuilder.EMAIL;
import static com.mb.modules.user.testdata.UserTestDataBuilder.FIRST_NAME;
import static com.mb.modules.user.testdata.UserTestDataBuilder.VALID_UUID;
import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUserResponseDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.common.exception.AppException;
import com.mb.modules.user.dao.UserDao;
import com.mb.modules.user.dto.response.UserResponseDto;
import com.mb.modules.user.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>Tests the service layer's delegation to the DAO layer for user operations. Verifies proper
 * exception propagation and correct parameter passing to DAO methods. Uses Mockito to isolate the
 * service from database concerns.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl – user retrieval service")
class UserServiceImplTest {

  @Mock private UserDao userDao;

  @InjectMocks private UserServiceImpl userService;

  // -------------------------------------------------------------------------
  // userByUuid - Happy path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("existing UUID → DTO returned from DAO with all fields populated")
  void userByUuidWithExistingUserReturnsDtoFromDao() {
    UserResponseDto expected = buildUserResponseDto();
    when(userDao.userByUuid(VALID_UUID, UserResponseDto.class)).thenReturn(expected);

    UserResponseDto result = userService.userByUuid(VALID_UUID);

    assertThat(result).isEqualTo(expected);
    assertThat(result.getUuid()).isEqualTo(VALID_UUID);
    assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
    assertThat(result.getEmail()).isEqualTo(EMAIL);
    verify(userDao).userByUuid(VALID_UUID, UserResponseDto.class);
  }

  // -------------------------------------------------------------------------
  // userByUuid - Error handling
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("DAO throws AppException → exception propagates unchanged to caller")
  void userByUuidWhenUserNotFoundPropagatesAppException() {
    when(userDao.userByUuid(VALID_UUID, UserResponseDto.class))
        .thenThrow(
            new AppException("User not found with uuid: " + VALID_UUID, HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> userService.userByUuid(VALID_UUID))
        .isInstanceOf(AppException.class)
        .hasMessageContaining(VALID_UUID.toString())
        .satisfies(
            e -> assertThat(((AppException) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  // -------------------------------------------------------------------------
  // Delegation verification
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("delegates to DAO with UUID and UserResponseDto.class projection")
  void userByUuidDelegatesToDaoWithCorrectProjectionClass() {
    UserResponseDto dto = buildUserResponseDto();
    when(userDao.userByUuid(VALID_UUID, UserResponseDto.class)).thenReturn(dto);

    userService.userByUuid(VALID_UUID);

    verify(userDao).userByUuid(VALID_UUID, UserResponseDto.class);
  }
}
