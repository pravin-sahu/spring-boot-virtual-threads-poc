package com.mb.modules.auth.identity.dao;

import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;

/**
 * @author rohit.kavthekar
 */
public interface UserIdentityDao {

  /**
   * Saves a user identity entity.
   *
   * @author rohit.kavthekar
   * @param identity the identity to persist
   * @return saved {@link UserIdentity}
   */
  UserIdentity save(UserIdentity identity);

  /**
   * Retrieves an identity by provider and the provider-issued subject ID. Throws {@link
   * com.mb.common.exception.AppException} with 404 status when not found.
   *
   * @author rohit.kavthekar
   * @param provider the authentication provider
   * @param providerUserId the subject/ID issued by the provider
   * @param targetClass projection or entity class to return
   * @param <T> return type
   * @return identity projected as {@code targetClass}
   */
  <T> T identityByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId, Class<T> targetClass);
}
