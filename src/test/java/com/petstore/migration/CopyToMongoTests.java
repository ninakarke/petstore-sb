package com.petstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.Order;
import com.petstore.order.OrderService;
import com.petstore.order.RequestedLine;
import com.petstore.shared.CopyReport;
import com.petstore.shared.internal.StoreMigrationRunner;
import com.petstore.testsupport.MongoContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The job that copies the relational database to MongoDB, run for real: a relational database with
 * data as the source, a MongoDB server in a container as the target.
 *
 * <p>The job runs once when the application starts with the {@code migrate-to-mongo} profile. The
 * test then looks at what arrived, places an order on the relational side and runs the job again.
 * Subclasses choose the relational database. Skipped when Docker is not available.
 */
@SpringBootTest(properties = "petstore.migration.exit-when-done=false")
@Testcontainers(disabledWithoutDocker = true)
@Import(MongoContainerConfiguration.class)
abstract class CopyToMongoTests {

  @Autowired private StoreMigrationRunner job;
  @Autowired private MongoTemplate mongo;
  @Autowired private OrderService orders;
  @Autowired private ApplicationContext context;

  @Test
  @DisplayName("everything is copied, the stores match, and a second run copies only what is new")
  void copiesEverythingAndKeepsUp() {
    // The first run happened at startup, after the seed data was loaded into the source.
    assertThat(count("categories")).isEqualTo(5);
    assertThat(count("products")).isEqualTo(16);
    assertThat(count("items")).isEqualTo(28);
    assertThat(count("users")).isEqualTo(4);
    assertThat(count("customers")).isEqualTo(4);

    // Running it again copies nothing and confirms that the two stores hold the same.
    Map<String, CopyReport> second = run();
    assertThat(second.keySet())
        .containsExactly(
            "categories", "products", "items", "users", "customers", "orders", "order counter");
    assertThat(second.values()).allMatch(CopyReport::matches);
    assertThat(second.values()).allMatch(report -> report.copied() == 0);
    assertThat(second.get("items").alreadyThere()).isEqualTo(28);
    assertThat(second.get("customers").inTarget()).isEqualTo(4);

    // The relational application takes one more order before the switch-over.
    Order order =
        orders
            .placeOrder("j2ee", List.of(new RequestedLine("EST-1", 2)), null, null, "before-switch")
            .order();

    Map<String, CopyReport> third = run();
    assertThat(third.values()).allMatch(CopyReport::matches);
    assertThat(third.get("orders").copied()).isEqualTo(1);
    assertThat(third.get("users").copied()).isZero();
    assertThat(third.get("order counter").inTarget())
        .isEqualTo(third.get("order counter").inSource());

    // The order is in MongoDB as one document, in the shape the MongoDB adapter writes.
    Document copied =
        mongo.getCollection("orders").find(new Document("_id", order.orderId())).first();
    assertThat(copied)
        .containsEntry("userId", "j2ee")
        .containsEntry("idempotencyKey", "before-switch")
        .containsEntry("totalPrice", Decimal128.parse("33.00"))
        .doesNotContainKey("_class");
    assertThat(copied.getList("lines", Document.class)).hasSize(1);
    assertThat(copied.get("handoff", Document.class))
        .containsEntry("status", order.handoff().status().name());
    assertThat(mongo.getCollection("counters").find().first().get("value"))
        .isEqualTo(third.get("order counter").inSource());
  }

  @Test
  @DisplayName("a customer's six relational rows arrive as one document")
  void customerArrivesAsOneDocument() {
    Document customer = mongo.getCollection("customers").find(new Document("_id", "j2ee")).first();

    assertThat(customer).containsOnlyKeys("_id", "account", "profile");
    assertThat(customer.get("account", Document.class).get("contact", Document.class))
        .containsKeys("givenName", "address");
  }

  @Test
  @DisplayName("during the copy the application itself still runs on the relational database")
  void applicationStoresStayRelational() {
    assertThat(context.getBeansWithAnnotation(Repository.class).values())
        .hasSize(5)
        .allSatisfy(adapter ->
            assertThat(AopUtils.getTargetClass(adapter).getPackageName())
                .endsWith(".persistence.jpa"));
  }

  private Map<String, CopyReport> run() {
    return job.migrate().stream()
        .collect(
            Collectors.toMap(
                CopyReport::what, Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));
  }

  private long count(String collection) {
    return mongo.getCollection(collection).countDocuments();
  }
}
