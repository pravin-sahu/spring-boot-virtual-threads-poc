package com.mb.modules.user.service;

import com.mb.modules.user.dao.UserDao;
import com.mb.modules.user.dto.response.UserResponseDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author rohit.kavthekar
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserDao userDao;

  /** {@inheritDoc} */
  @Override
  public UserResponseDto userByUuid(UUID userUuid) {

    return userDao.userByUuid(userUuid, UserResponseDto.class);
  }
}
