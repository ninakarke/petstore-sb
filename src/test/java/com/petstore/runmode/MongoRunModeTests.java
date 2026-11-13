package com.petstore.runmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.petstore.testsupport.MongoContainerConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.health.MongoHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The {@code mongo} profile switches on MongoDB and nothing of the relational stack, and the
 * application starts and loads its seed data through the same loaders. Skipped when Docker is not
 * available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mongo")
@Testcontainers(disabledWithoutDocker = true)
@Import(MongoContainerConfiguration.class)
class MongoRunModeTests {

  @Autowired private ApplicationContext context;
  @Autowired private MongoTemplate mongo;
  @Autowired private MockMvcTester mvc;

  @Test
  @DisplayName("with the mongo profile there is a MongoDB client and no relational database")
  void documentStackOnly() {
    assertThat(context.getBeanNamesForType(MongoClient.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
  }

  @Test
  @DisplayName("with the mongo profile every persistence adapter is a MongoDB one")
  void everyAdapterIsADocumentAdapter() {
    assertThat(context.getBeansWithAnnotation(Repository.class).values())
        .hasSize(5)
        .allSatisfy(adapter ->
            assertThat(AopUtils.getTargetClass(adapter).getPackageName())
                .endsWith(".persistence.mongo"));
  }

  @Test
  @DisplayName("the seed loaders fill MongoDB through the ports, unchanged")
  void seedDataIsLoaded() {
    assertThat(mongo.getCollection("categories").countDocuments()).isEqualTo(5);
    assertThat(mongo.getCollection("products").countDocuments()).isEqualTo(16);
    assertThat(mongo.getCollection("items").countDocuments()).isEqualTo(28);
    assertThat(mongo.getCollection("users").countDocuments()).isGreaterThanOrEqualTo(4);
    assertThat(mongo.getCollection("customers").countDocuments()).isGreaterThanOrEqualTo(4);
    assertThat(mongo.getCollection("counters").countDocuments()).isEqualTo(1);
  }

  @Test
  @DisplayName("the health check is up and includes MongoDB")
  void healthIncludesMongo() {
    assertThat(context.getBeanNamesForType(MongoHealthIndicator.class)).hasSize(1);
    assertThat(mvc.get().uri("/actuator/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("UP");
  }
}
