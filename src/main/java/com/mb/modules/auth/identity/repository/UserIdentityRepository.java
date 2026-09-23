package com.mb.modules.auth.identity.repository;

import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author rohit.kavthekar
 */
public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

  <T> Optional<T> findByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId, Class<T> targetClass);
}
