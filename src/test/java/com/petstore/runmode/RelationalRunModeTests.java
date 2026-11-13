package com.petstore.runmode;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

/**
 * The default run mode switches on the relational stack and nothing of the MongoDB one.
 *
 * <p>Both stacks are on the classpath. Without this check, a MongoDB client could quietly be
 * created in the relational run modes and keep trying to reach a server that is not there.
 */
@SpringBootTest
class RelationalRunModeTests {

  @Autowired private ApplicationContext context;

  @Test
  @DisplayName("by default there is a relational database and no MongoDB client")
  void relationalStackOnly() {
    assertThat(context.getBeanNamesForType(DataSource.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(MongoClient.class)).isEmpty();
  }

  @Test
  @DisplayName("by default every persistence adapter is a relational one")
  void everyAdapterIsRelational() {
    assertThat(context.getBeansWithAnnotation(Repository.class).values())
        .hasSize(5)
        .allSatisfy(adapter ->
            assertThat(AopUtils.getTargetClass(adapter).getPackageName())
                .endsWith(".persistence.jpa"));
  }
}
