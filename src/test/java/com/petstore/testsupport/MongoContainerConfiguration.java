package com.petstore.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Starts a real MongoDB server in a container for a test and connects the application to it.
 *
 * <p>The container lives as long as the Spring test context that imports this configuration. The
 * image matches {@code compose.yaml}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoContainerConfiguration {

  @Bean
  @ServiceConnection
  MongoDBContainer mongoContainer() {
    return new MongoDBContainer("mongo:8");
  }
}
