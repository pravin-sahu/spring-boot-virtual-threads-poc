package com.mb.modules.auth.identity.entity;

import com.mb.common.enums.EntityStatus;
import com.mb.infrastructure.persistence.entity.BaseEntity;
import com.mb.modules.auth.identity.enums.AuthProvider;
import com.mb.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Represents one login method (identity) for a {@link User}. A single user can have multiple
 * identities across different authentication providers (LOCAL, GOOGLE, FACEBOOK, GITHUB). The
 * combination of {@code provider} and {@code providerUserId} is globally unique.
 *
 * @author rohit.kavthekar
 */
@Entity
@Table(
    name = "user_identities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_user_identities_provider",
            columnNames = {"provider", "provider_user_id"}))
@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class UserIdentity extends BaseEntity {

  private static final long serialVersionUID = 1L;

  @EqualsAndHashCode.Include
  @Column(nullable = false, updatable = false)
  private UUID uuid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AuthProvider provider;

  /**
   * The subject/ID issued by the provider (Auth0, Google sub, Facebook UID, or email for LOCAL).
   */
  @Column(nullable = false)
  private String providerUserId;

  /** Email from the provider — used to auto-link identities that share the same email address. */
  @Column private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EntityStatus status;
}
