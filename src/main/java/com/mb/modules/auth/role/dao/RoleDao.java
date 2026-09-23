package com.mb.modules.auth.role.dao;

import com.mb.modules.auth.role.entity.Role;

/**
 * @author rohit.kavthekar
 */
public interface RoleDao {

  /**
   * Role by name
   *
   * @author rohit.kavthekar
   * @param <T>
   * @param name must be unique and not null
   * @param targetClass
   * @return {@link Role}
   */
  <T> T roleByName(String name, Class<T> targetClass);
}
