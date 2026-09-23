package com.mb.modules.user.dao;

import com.mb.modules.user.entity.User;
import java.util.UUID;

/**
 * @author rohit.kavthekar
 */
public interface UserDao {

  /**
   * Saves a user entity. Use the returned instance for further operations as the save operation
   * might have changed the entity instance completely.
   *
   * @author rohit.kavthekar
   * @param user
   * @return {@link User}
   */
  User saveUser(User user);

  /**
   * Retrieve user by email
   *
   * @author rohit.kavthekar
   * @param <T>
   * @param email
   * @param targetClass
   * @return {@link User}
   */
  <T> T userByEmail(String email, Class<T> targetClass);

  /**
   * User by uuid
   *
   * @author rohit.kavthekar
   * @param userUuid
   * @param targetClass
   * @param <T>
   * @return {@link User}
   */
  <T> T userByUuid(UUID userUuid, Class<T> targetClass);
}
