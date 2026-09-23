package com.mb.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for full-stack integration tests that require a PostgreSQL database.
 *
 * <p>Imports {@link TestContainersConfig} which declares a {@code @ServiceConnection} PostgreSQL
 * container — Spring Boot auto-configures the datasource from the container's connection details
 * without any manual {@code @DynamicPropertySource} wiring.
 *
 * <p>{@code @AutoConfigureMockMvc} activates {@code MockMvcAutoConfiguration} which creates a
 * {@code MockMvc} bean and applies all {@code MockMvcBuilderCustomizer} beans — including the
 * {@link MockMvcSecurityConfig} customizer that calls {@code springSecurity()}. Without this
 * annotation, {@code RestTestClientTestAutoConfiguration} falls back to {@code
 * RestTestClient.bindToApplicationContext()} which builds its own internal MockMvc that bypasses
 * customizers entirely, and unauthenticated requests reach the controller instead of returning 401.
 *
 * @author rohit.kavthekar
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfig.class, MockMvcSecurityConfig.class})
@ActiveProfiles("test")
public abstract class AbstractBaseIntegrationTest {}
