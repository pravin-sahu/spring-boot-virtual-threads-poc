package com.mb.modules.auth.role.repository;

import com.mb.modules.auth.role.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author rohit.kavthekar
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

  <T> Optional<T> findByName(String name, Class<T> targetClass);
}
