package com.mb.modules.auth.identity.testdata;

import static com.mb.modules.user.testdata.UserTestDataBuilder.buildUser;

import com.mb.common.enums.EntityStatus;
import com.mb.modules.auth.identity.entity.UserIdentity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import com.mb.modules.user.entity.User;
import java.util.UUID;

/**
 * Central test-data factory for {@link UserIdentity}-related tests.
 *
 * <ul>
 *   <li>{@link #buildLocalIdentity(User)} — LOCAL provider (username/password).
 *   <li>{@link #buildAuth0Identity(User)} — AUTH0 provider (social logins via Auth0).
 *   <li>{@link #buildIdentity(User, AuthProvider, String)} — arbitrary provider and subject ID.
 * </ul>
 *
 * @author rohit.kavthekar
 */
public class UserIdentityTestDataBuilder {

  public static final String EMAIL = "john.doe@example.com";
  public static final String LOCAL_PROVIDER_USER_ID = EMAIL;
  public static final String AUTH0_PROVIDER_USER_ID = "auth0|112233445566aabbccdd";
  public static final String NONEXISTENT_PROVIDER_USER_ID = "auth0|nonexistent000000";

  private UserIdentityTestDataBuilder() {}

  /**
   * Returns a LOCAL identity with a fixed UUID, linked to the given user. Use in unit tests where
   * the UUID must match a Mockito stub.
   */
  public static UserIdentity buildLocalIdentity(User user) {
    UserIdentity identity = new UserIdentity();
    identity.setUuid(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));
    identity.setUser(user);
    identity.setProvider(AuthProvider.LOCAL);
    identity.setProviderUserId(LOCAL_PROVIDER_USER_ID);
    identity.setEmail(EMAIL);
    identity.setStatus(EntityStatus.ACTIVE);
    return identity;
  }

  /**
   * Returns an AUTH0 identity with a fixed UUID, linked to the given user. Use in unit tests where
   * the UUID must match a Mockito stub.
   */
  public static UserIdentity buildAuth0Identity(User user) {
    UserIdentity identity = new UserIdentity();
    identity.setUuid(UUID.fromString("660e8400-e29b-41d4-a716-446655440002"));
    identity.setUser(user);
    identity.setProvider(AuthProvider.AUTH0);
    identity.setProviderUserId(AUTH0_PROVIDER_USER_ID);
    identity.setEmail(EMAIL);
    identity.setStatus(EntityStatus.ACTIVE);
    return identity;
  }

  /**
   * Returns an identity with a random UUID for the given provider and subject ID. Use in
   * integration/repository tests where multiple rows must have unique identifiers.
   */
  public static UserIdentity buildIdentity(
      User user, AuthProvider provider, String providerUserId) {
    UserIdentity identity = new UserIdentity();
    identity.setUuid(UUID.randomUUID());
    identity.setUser(user);
    identity.setProvider(provider);
    identity.setProviderUserId(providerUserId);
    identity.setEmail(EMAIL);
    identity.setStatus(EntityStatus.ACTIVE);
    return identity;
  }

  /** Convenience factory that builds a default user and attaches a LOCAL identity to it. */
  public static UserIdentity buildLocalIdentity() {
    return buildLocalIdentity(buildUser());
  }

  /** Convenience factory that builds a default user and attaches an AUTH0 identity to it. */
  public static UserIdentity buildAuth0Identity() {
    return buildAuth0Identity(buildUser());
  }

  /**
   * Returns a LOCAL identity with {@link EntityStatus#INACTIVE} status, linked to the given user.
   * Use to test authentication/authorisation paths that must reject inactive identities.
   */
  public static UserIdentity buildInactiveLocalIdentity(User user) {
    UserIdentity identity = buildLocalIdentity(user);
    identity.setStatus(EntityStatus.INACTIVE);
    return identity;
  }

  /** Convenience factory that builds a default user and attaches an inactive LOCAL identity. */
  public static UserIdentity buildInactiveLocalIdentity() {
    return buildInactiveLocalIdentity(buildUser());
  }
}
