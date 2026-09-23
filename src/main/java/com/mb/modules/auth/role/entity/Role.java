package com.mb.modules.auth.role.entity;

import com.mb.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "roles")
@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Role extends BaseEntity {

  private static final long serialVersionUID = 1L;

  @EqualsAndHashCode.Include
  @Column(nullable = false, updatable = false)
  private UUID uuid;

  @Column(nullable = false, unique = true)
  private String name;

  @Column private String description;
}
