package com.mb.modules.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author rohit.kavthekar
 */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponseDto {

  private UUID uuid;

  private String firstName;

  private String lastName;

  private String email;
}
