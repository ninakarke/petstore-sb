package com.petstore.order.internal.persistence.mongo;

import static com.petstore.order.OrderFixtures.NOON;
import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.OrderFixtures;
import com.petstore.order.internal.OrderStore;
import com.petstore.order.internal.OrderStoreContract;
import com.petstore.testsupport.MongoStoreTest;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;

/**
 * The order store contract on a real MongoDB server, followed by what is specific to this store.
 */
@MongoStoreTest
@Import(MongoOrderStore.class)
class MongoOrderStoreTests extends OrderStoreContract {

  @Autowired private OrderStore store;
  @Autowired private MongoTemplate mongo;

  @Override
  protected OrderStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    mongo.remove(new Query(), "orders");
  }

  @Test
  @DisplayName("an order is one document: lines, addresses, card and hand-off state inside it")
  void orderIsOneDocument() {
    store.save(OrderFixtures.order("10011", "j2ee", NOON));

    Document order = mongo.getCollection("orders").find(new Document("_id", "10011")).first();

    assertThat(order)
        .containsKeys("userId", "placedAt", "shipTo", "billTo", "creditCard", "lines", "handoff")
        .doesNotContainKeys("_class", "idempotencyKey");
    assertThat(order.getList("lines", Document.class))
        .extracting(line -> line.getString("itemId"))
        .containsExactly("EST-1", "EST-6");
    assertThat(order.get("handoff", Document.class))
        .containsEntry("status", "PENDING")
        .containsEntry("attempts", 0);
    assertThat(mongo.getCollectionNames()).doesNotContain("order_line", "orderLines", "outbox");
  }

  @Test
  @DisplayName("amounts are stored as decimal numbers")
  void amountsAreStoredAsDecimals() {
    store.save(OrderFixtures.order("10011", "j2ee", NOON));

    Document order = mongo.getCollection("orders").find(new Document("_id", "10011")).first();

    assertThat(order.get("totalPrice")).isEqualTo(Decimal128.parse("68.00"));
    assertThat(order.getList("lines", Document.class).get(0).get("unitPrice"))
        .isEqualTo(Decimal128.parse("16.50"));
  }

  @Test
  @DisplayName("the retry job's index holds only orders that are still to be delivered")
  void pendingHandoffIndexIsPartial() {
    List<IndexInfo> indexes = mongo.indexOps("orders").getIndexInfo();

    assertThat(indexes)
        .extracting(IndexInfo::getName)
        .contains("by_user_newest_first", "idempotency_key_per_user", "pending_handoff");
    IndexInfo pending =
        indexes.stream()
            .filter(i -> i.getName().equals("pending_handoff"))
            .findFirst()
            .orElseThrow();
    assertThat(pending.getPartialFilterExpression()).contains("handoff.status").contains("PENDING");
    IndexInfo idempotency =
        indexes.stream()
            .filter(i -> i.getName().equals("idempotency_key_per_user"))
            .findFirst()
            .orElseThrow();
    assertThat(idempotency.isUnique()).isTrue();
  }

  @Test
  @DisplayName("the list of undelivered orders is read from its index, not by scanning all orders")
  void pendingOrdersAreFoundThroughTheIndex() {
    store.save(OrderFixtures.order("10011", "j2ee", NOON));

    Document plan =
        mongo
            .getCollection("orders")
            .find(new Document("handoff.status", "PENDING"))
            .sort(new Document("placedAt", 1).append("_id", 1))
            .explain();

    assertThat(plan.toJson()).contains("pending_handoff").doesNotContain("COLLSCAN");
  }
}
