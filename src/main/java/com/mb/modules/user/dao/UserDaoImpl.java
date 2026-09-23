package com.mb.modules.user.dao;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.common.exception.ErrorCode;
import com.mb.modules.user.entity.User;
import com.mb.modules.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * @author rohit.kavthekar
 */
@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

  private final UserRepository userRepo;

  /** {@inheritDoc} */
  @Override
  public User saveUser(User user) {
    try {
      return userRepo.save(user);
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
  public <T> T userByEmail(String email, Class<T> targetClass) {

    return userRepo
        .findByEmail(email, targetClass)
        .orElseThrow(() -> new AppException(ExceptionMessage.USER_NOT_FOUND, HttpStatus.NOT_FOUND));
  }

  /** {@inheritDoc} */
  @Override
  public <T> T userByUuid(UUID userUuid, Class<T> targetClass) {

    return userRepo
        .findByUuid(userUuid, targetClass)
        .orElseThrow(
            () ->
                new AppException(
                    ExceptionMessage.USER_NOT_FOUND_BY_UUID + userUuid, HttpStatus.NOT_FOUND));
  }
}
