package com.mb.modules.auth.identity.dao;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import com.mb.modules.auth.identity.repository.UserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * @author rohit.kavthekar
 */
@Repository
@RequiredArgsConstructor
public class UserIdentityDaoImpl implements UserIdentityDao {

  private final UserIdentityRepository userIdentityRepo;

  /** {@inheritDoc} */
  @Override
  public UserIdentity save(UserIdentity identity) {
    try {
      return userIdentityRepo.save(identity);
    } catch (DataIntegrityViolationException e) {
      throw new AppException(
          ExceptionMessage.DUPLICATE_RESOURCE,
          e.getMessage(),
          HttpStatus.CONFLICT,
          ErrorCode.DUPLICATE_RESOURCE);
    } catch (Exception e) {
      throw new AppException(
          ExceptionMessage.INTERNAL_SERVER_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /** {@inheritDoc} */
  @Override
  public <T> T identityByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId, Class<T> targetClass) {

    return userIdentityRepo
        .findByProviderAndProviderUserId(provider, providerUserId, targetClass)
        .orElseThrow(
            () ->
                new AppException(
                    ExceptionMessage.IDENTITY_NOT_FOUND + provider + " / " + providerUserId,
                    HttpStatus.NOT_FOUND));
  }
}
