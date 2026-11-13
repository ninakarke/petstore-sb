package com.petstore.customer.internal.persistence.mongo;

import com.petstore.customer.internal.CustomerCopy;
import com.petstore.customer.internal.CustomerStore;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreMigration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * The customer module's part of the copy to MongoDB: six rows in six tables become one document.
 */
@Component
@Profile(StoreMigration.PROFILE)
class MongoCustomerMigration implements StoreMigration {

  private final CustomerStore source;
  private final MongoCustomerStore target;

  MongoCustomerMigration(CustomerStore source, MongoOperations mongo) {
    this.source = source;
    this.target = new MongoCustomerStore(mongo);
  }

  @Override
  public int order() {
    return 30;
  }

  @Override
  public List<CopyReport> run(int batchSize) {
    return CustomerCopy.copy(source, target, batchSize);
  }
}
