package com.mb.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration – isolated in its own {@code @Configuration} class so that {@code @WebMvcTest}
 * slices (which have no JPA context) do not attempt to initialise {@code @EnableJpaAuditing} and
 * fail with "JPA metamodel must not be empty".
 *
 * @author rohit.kavthekar
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class JpaConfig {}
