package com.petstore.order.internal.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.petstore.order.internal.OrderIdGenerator;
import com.petstore.order.internal.OrderIdGeneratorContract;
import com.petstore.testsupport.MongoStoreTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

/** The order-number contract on a real MongoDB server, including the concurrency test. */
@MongoStoreTest
@Import(MongoOrderIdGenerator.class)
class MongoOrderIdGeneratorTests extends OrderIdGeneratorContract {

  @Autowired private OrderIdGenerator generator;
  @Autowired private MongoTemplate mongo;

  @Override
  protected OrderIdGenerator generator() {
    return generator;
  }

  @Override
  protected void resetTheCounter() {
    mongo.upsert(
        query(where("_id").is(OrderIdGenerator.PREFIX)), Update.update("value", 0L), "counters");
  }

  @Test
  @DisplayName("starting the application again does not reset the counter")
  void startingAgainKeepsTheCounter() {
    generator.nextOrderId();
    generator.nextOrderId();

    ((MongoOrderIdGenerator) generator).afterPropertiesSet();

    assertThat(generator.nextOrderId()).isEqualTo("10013");
  }
}
