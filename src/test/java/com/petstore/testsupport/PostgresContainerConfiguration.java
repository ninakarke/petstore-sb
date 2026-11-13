package com.petstore.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a real PostgreSQL server in a container for a test and connects the application to it.
 *
 * <p>The container lives as long as the Spring test context that imports this configuration, so
 * test classes that share a context share one server. The image matches {@code compose.yaml}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer("postgres:18");
  }
}
