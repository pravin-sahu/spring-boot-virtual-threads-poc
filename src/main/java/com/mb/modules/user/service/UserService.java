package com.mb.modules.user.service;

import com.mb.modules.user.dto.response.UserResponseDto;
import java.util.UUID;

/**
 * @author rohit.kavthekar
 */
public interface UserService {

  /**
   * User by uuid
   *
   * @author rohit.kavthekar
   * @param userUuid
   * @return {@link UserResponseDto}
   */
  UserResponseDto userByUuid(UUID userUuid);
}
