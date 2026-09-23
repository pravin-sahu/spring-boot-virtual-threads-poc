package com.mb.modules.auth.identity.unit.dao;

import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.AUTH0_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.LOCAL_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.NONEXISTENT_PROVIDER_USER_ID;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.buildAuth0Identity;
import static com.mb.modules.auth.identity.testdata.UserIdentityTestDataBuilder.buildLocalIdentity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.modules.auth.identity.dao.UserIdentityDaoImpl;
import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import com.mb.modules.auth.identity.repository.UserIdentityRepository;
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
 * Unit tests for {@link UserIdentityDaoImpl}.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserIdentityDaoImpl – identity persistence and lookup operations")
class UserIdentityDaoImplTest {

  @Mock private UserIdentityRepository userIdentityRepo;

  @InjectMocks private UserIdentityDaoImpl userIdentityDao;

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("save: valid identity → saved entity returned and repository.save called once")
  void saveWithValidIdentityReturnsSavedEntity() {
    UserIdentity identity = buildLocalIdentity();
    when(userIdentityRepo.save(identity)).thenReturn(identity);

    UserIdentity result = userIdentityDao.save(identity);

    assertThat(result).isEqualTo(identity);
    verify(userIdentityRepo).save(identity);
  }

  @Test
  @DisplayName("save: repository throws RuntimeException → wrapped in AppException with 500 status")
  void saveWhenRepoThrowsRuntimeExceptionWrapsInAppException() {
    UserIdentity identity = buildLocalIdentity();
    when(userIdentityRepo.save(identity)).thenThrow(new RuntimeException("connection lost"));

    assertThatThrownBy(() -> userIdentityDao.save(identity))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage()).isEqualTo(ExceptionMessage.INTERNAL_SERVER_ERROR);
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              assertThat(ae.getDetail()).isEqualTo("connection lost");
            });
  }

  @Test
  @DisplayName(
      "save: DataIntegrityViolationException (duplicate provider+providerUserId) → wrapped in"
          + " AppException with 409 CONFLICT status")
  void saveWhenDataIntegrityViolationWrapsInAppException() {
    UserIdentity identity = buildLocalIdentity();
    when(userIdentityRepo.save(identity))
        .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

    assertThatThrownBy(() -> userIdentityDao.save(identity))
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
  // identityByProviderAndProviderUserId
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "identityByProviderAndProviderUserId: existing LOCAL identity → entity returned and repo"
          + " called with correct args")
  void identityByProviderAndProviderUserIdWhenLocalIdentityExistsReturnsEntity() {
    UserIdentity identity = buildLocalIdentity();
    when(userIdentityRepo.findByProviderAndProviderUserId(
            AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class))
        .thenReturn(Optional.of(identity));

    UserIdentity result =
        userIdentityDao.identityByProviderAndProviderUserId(
            AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(result).isEqualTo(identity);
    assertThat(result.getProvider()).isEqualTo(AuthProvider.LOCAL);
    verify(userIdentityRepo)
        .findByProviderAndProviderUserId(
            AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class);
  }

  @Test
  @DisplayName(
      "identityByProviderAndProviderUserId: existing AUTH0 identity → entity returned with correct"
          + " provider")
  void identityByProviderAndProviderUserIdWhenAuth0IdentityExistsReturnsEntity() {
    UserIdentity identity = buildAuth0Identity();
    when(userIdentityRepo.findByProviderAndProviderUserId(
            AuthProvider.AUTH0, AUTH0_PROVIDER_USER_ID, UserIdentity.class))
        .thenReturn(Optional.of(identity));

    UserIdentity result =
        userIdentityDao.identityByProviderAndProviderUserId(
            AuthProvider.AUTH0, AUTH0_PROVIDER_USER_ID, UserIdentity.class);

    assertThat(result).isEqualTo(identity);
    assertThat(result.getProvider()).isEqualTo(AuthProvider.AUTH0);
  }

  @Test
  @DisplayName(
      "identityByProviderAndProviderUserId: unknown provider/subject → AppException with 404 status"
          + " and provider in message")
  void identityByProviderAndProviderUserIdWhenNotFoundThrowsAppExceptionWith404() {
    when(userIdentityRepo.findByProviderAndProviderUserId(any(), anyString(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                userIdentityDao.identityByProviderAndProviderUserId(
                    AuthProvider.AUTH0, NONEXISTENT_PROVIDER_USER_ID, UserIdentity.class))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getMessage())
                  .startsWith(ExceptionMessage.IDENTITY_NOT_FOUND)
                  .contains(AuthProvider.AUTH0.name());
              assertThat(ae.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  @DisplayName(
      "identityByProviderAndProviderUserId: repository throws RuntimeException → propagates"
          + " unwrapped")
  void identityByProviderAndProviderUserIdWhenRepoThrowsRuntimeExceptionPropagatesUnwrapped() {
    RuntimeException dbException = new RuntimeException("Database connection lost");
    when(userIdentityRepo.findByProviderAndProviderUserId(
            eq(AuthProvider.LOCAL), anyString(), any()))
        .thenThrow(dbException);

    assertThatThrownBy(
            () ->
                userIdentityDao.identityByProviderAndProviderUserId(
                    AuthProvider.LOCAL, LOCAL_PROVIDER_USER_ID, UserIdentity.class))
        .isInstanceOf(RuntimeException.class)
        .isNotInstanceOf(AppException.class)
        .hasMessage("Database connection lost");
  }
}
