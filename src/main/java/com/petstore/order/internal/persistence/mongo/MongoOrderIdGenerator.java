package com.petstore.order.internal.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.petstore.order.internal.OrderIdGenerator;
import com.petstore.shared.DocumentStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * MongoDB adapter of the order-number generator.
 *
 * <p>The relational adapter locks the counter row while it adds one. Here "add one and tell me the
 * result" is a single operation that the server applies atomically, so no lock is needed and two
 * requests can never be given the same number.
 */
@Repository
@DocumentStore
class MongoOrderIdGenerator implements OrderIdGenerator, InitializingBean {

  private static final Query THE_COUNTER = query(where("_id").is(PREFIX));

  private final MongoOperations mongo;

  MongoOrderIdGenerator(MongoOperations mongo) {
    this.mongo = mongo;
  }

  /**
   * Creates the counter at zero if it does not exist yet, and leaves it alone if it does. The
   * relational schema gets the same starting row from its migration.
   */
  @Override
  public void afterPropertiesSet() {
    mongo.upsert(THE_COUNTER, new Update().setOnInsert("value", 0L), CounterDocument.class);
  }

  @Override
  public String nextOrderId() {
    CounterDocument counter =
        mongo.findAndModify(
            THE_COUNTER,
            new Update().inc("value", 1L),
            FindAndModifyOptions.options().returnNew(true),
            CounterDocument.class);
    if (counter == null) {
      throw new IllegalStateException("The order counter '" + PREFIX + "' is missing");
    }
    // Legacy: getName() + String.valueOf(count)
    return PREFIX + counter.value();
  }
}
