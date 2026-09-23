package com.mb.modules.user.repository;

import com.mb.modules.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author rohit.kavthekar
 */
public interface UserRepository extends JpaRepository<User, Long> {

  <T> Optional<T> findByEmail(String email, Class<T> targetClass);

  <T> Optional<T> findByUuid(UUID uuid, Class<T> targetClass);
}
