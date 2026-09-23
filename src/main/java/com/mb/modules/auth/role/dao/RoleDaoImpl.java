package com.mb.modules.auth.role.dao;

import com.mb.common.constant.ExceptionMessage;
import com.mb.common.exception.AppException;
import com.mb.modules.auth.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * @author rohit.kavthekar
 */
@Repository
@RequiredArgsConstructor
public class RoleDaoImpl implements RoleDao {

  private final RoleRepository roleRepo;

  /** {@inheritDoc} */
  @Override
  public <T> T roleByName(String name, Class<T> targetClass) {

    return roleRepo
        .findByName(name, targetClass)
        .orElseThrow(
            () -> new AppException(ExceptionMessage.ROLE_NOT_FOUND + name, HttpStatus.NOT_FOUND));
  }
}
