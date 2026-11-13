package com.petstore.order.internal.persistence.mongo;

import com.petstore.order.internal.OrderCopy;
import com.petstore.order.internal.OrderIdGenerator;
import com.petstore.order.internal.OrderStore;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreMigration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * The order module's part of the copy to MongoDB: orders with their lines and hand-off state, and
 * the order counter.
 */
@Component
@Profile(StoreMigration.PROFILE)
class MongoOrderMigration implements StoreMigration {

  private final OrderStore source;
  private final OrderIdGenerator sourceNumbers;
  private final MongoOrderStore target;
  private final MongoOrderIdGenerator targetNumbers;

  MongoOrderMigration(OrderStore source, OrderIdGenerator sourceNumbers, MongoOperations mongo) {
    this.source = source;
    this.sourceNumbers = sourceNumbers;
    this.target = new MongoOrderStore(mongo);
    this.target.afterPropertiesSet();
    this.targetNumbers = new MongoOrderIdGenerator(mongo);
    this.targetNumbers.afterPropertiesSet();
  }

  @Override
  public int order() {
    return 40;
  }

  @Override
  public List<CopyReport> run(int batchSize) {
    return OrderCopy.copy(source, sourceNumbers, target, targetNumbers, batchSize);
  }
}
