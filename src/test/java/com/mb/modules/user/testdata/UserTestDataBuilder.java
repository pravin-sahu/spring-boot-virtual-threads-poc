package com.mb.modules.user.testdata;

import com.mb.modules.user.dto.response.UserResponseDto;
import com.mb.modules.user.entity.User;
import java.util.UUID;

/**
 * Central test-data factory for user-related tests.
 *
 * <ul>
 *   <li>Constants expose well-known, stable values so test classes don't re-declare them.
 *   <li>{@link #buildUser()} returns a {@link User} with a fixed UUID – use in unit tests where the
 *       UUID must match a mock expectation.
 *   <li>{@link #buildUser(String)} returns a {@link User} with a random UUID – use in integration /
 *       repository tests where multiple rows are inserted and uniqueness is required.
 * </ul>
 *
 * @author rohit.kavthekar
 */
public class UserTestDataBuilder {

  public static final UUID VALID_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  public static final String FIRST_NAME = "John";
  public static final String LAST_NAME = "Doe";
  public static final String EMAIL = "john.doe@example.com";
  public static final String USERS_BY_UUID_URL = "/v1/users/{uuid}";

  private UserTestDataBuilder() {}

  /**
   * Returns a {@link User} with {@link #VALID_UUID} and {@link #EMAIL}. Suited for unit tests where
   * the UUID must match a Mockito stub.
   */
  public static User buildUser() {
    User user = new User();
    user.setUuid(VALID_UUID);
    user.setEmail(EMAIL);
    user.setFirstName(FIRST_NAME);
    user.setLastName(LAST_NAME);
    return user;
  }

  /**
   * Returns a {@link User} with a random UUID and the supplied {@code email}. Suited for
   * integration/repository tests where multiple rows must have unique identifiers.
   */
  public static User buildUser(String email) {
    User user = new User();
    user.setUuid(UUID.randomUUID());
    user.setEmail(email);
    user.setFirstName(FIRST_NAME);
    user.setLastName(LAST_NAME);
    return user;
  }

  /**
   * Returns a {@link User} with a random UUID, the supplied {@code email}, and {@code null} first /
   * last name. Suited for testing nullable-name edge cases.
   */
  public static User buildUserWithNullNames(String email) {
    User user = new User();
    user.setUuid(UUID.randomUUID());
    user.setEmail(email);
    return user;
  }

  /** Returns a fully-populated {@link UserResponseDto} using the standard constant values. */
  public static UserResponseDto buildUserResponseDto() {
    return UserResponseDto.builder()
        .uuid(VALID_UUID)
        .firstName(FIRST_NAME)
        .lastName(LAST_NAME)
        .email(EMAIL)
        .build();
  }
}
