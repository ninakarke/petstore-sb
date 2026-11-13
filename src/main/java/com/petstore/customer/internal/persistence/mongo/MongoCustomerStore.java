package com.petstore.customer.internal.persistence.mongo;

import com.petstore.customer.Customer;
import com.petstore.customer.internal.CustomerStore;
import com.petstore.shared.DocumentStore;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * MongoDB adapter of the customer port.
 *
 * <p>Replaces the six container-managed entity beans of the legacy customer component. Reading a
 * customer is one lookup by key; saving one replaces the whole document in a single atomic write.
 */
@Repository
@DocumentStore
class MongoCustomerStore implements CustomerStore {

  private final MongoOperations mongo;

  MongoCustomerStore(MongoOperations mongo) {
    this.mongo = mongo;
  }

  @Override
  public Optional<Customer> find(String userId) {
    return Optional.ofNullable(mongo.findById(userId, CustomerDocument.class))
        .map(CustomerDocument::toCustomer);
  }

  @Override
  public void save(Customer customer) {
    mongo.save(CustomerDocument.of(customer));
  }

  @Override
  public boolean isEmpty() {
    return !mongo.exists(new Query(), CustomerDocument.class);
  }
}
