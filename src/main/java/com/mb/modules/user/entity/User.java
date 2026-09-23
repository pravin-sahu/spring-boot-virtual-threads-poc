package com.mb.modules.user.entity;

import com.mb.infrastructure.persistence.entity.BaseEntity;
import com.mb.modules.auth.role.entity.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * @author rohit.kavthekar
 */
@Entity
@Table(name = "users")
@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class User extends BaseEntity {

  private static final long serialVersionUID = 1L;

  @EqualsAndHashCode.Include
  @Column(nullable = false, updatable = false)
  private UUID uuid;

  @Column private String firstName;

  @Column private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true,
      mappedBy = "user")
  private List<UserRole> userRoles;
}
