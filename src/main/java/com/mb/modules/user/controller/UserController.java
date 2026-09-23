package com.mb.modules.user.controller;

import com.mb.common.constant.ApiEndpoint;
import com.mb.common.constant.ResponseMessage;
import com.mb.common.dto.response.ApiResponse;
import com.mb.common.util.ApiResponseBuilder;
import com.mb.modules.user.dto.response.UserResponseDto;
import com.mb.modules.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User related api's controller
 *
 * @author rohit.kavthekar
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiEndpoint.USERS)
public class UserController {

  private final ApiResponseBuilder responseBuilder;

  private final UserService userService;

  /**
   * User by uuid
   *
   * @author rohit.kavthekar
   * @return {@link ResponseEntity}
   */
  @GetMapping(value = ApiEndpoint.USER_BY_UUID, version = "1.0")
  public ResponseEntity<ApiResponse<UserResponseDto>> userByUuid(@PathVariable UUID userUuid) {

    UserResponseDto userResponseDto = userService.userByUuid(userUuid);

    return responseBuilder.success(ResponseMessage.SUCCESS, userResponseDto, HttpStatus.OK);
  }
}
