package com.mb.base;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for JPA slice tests that require a PostgreSQL database.
 *
 * <p>Loads only the JPA slice ({@code @DataJpaTest}) and imports {@link TestContainersConfig} for
 * the {@code @ServiceConnection} PostgreSQL container and {@link TestJpaConfig} for auditing setup.
 * Spring Boot auto-configures the datasource from the container — no manual
 * {@code @DynamicPropertySource} wiring needed.
 *
 * @author rohit.kavthekar
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestContainersConfig.class, TestJpaConfig.class})
@ActiveProfiles("test")
public abstract class AbstractBaseJpaTest {}
