package com.mb.base;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for integration tests.
 *
 * <p>{@code @ServiceConnection} on the {@code @Bean} method lets Spring Boot automatically read the
 * container's connection details and configure the datasource — no manual
 * {@code @DynamicPropertySource} wiring needed.
 *
 * @author rohit.kavthekar
 */
@TestConfiguration
public class TestContainersConfig {

  @Bean
  @ServiceConnection
  @SuppressWarnings("resource")
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
  }
}
