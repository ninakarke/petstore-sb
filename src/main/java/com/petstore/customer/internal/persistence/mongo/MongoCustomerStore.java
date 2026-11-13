package com.petstore.customer.internal.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.petstore.customer.Customer;
import com.petstore.customer.internal.CustomerStore;
import com.petstore.shared.DocumentStore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
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
  public List<Customer> customersFrom(String startUserId, int limit) {
    Query query = startUserId == null ? new Query() : query(where("_id").gte(startUserId));
    return mongo.find(query.with(Sort.by("_id")).limit(limit), CustomerDocument.class).stream()
        .map(CustomerDocument::toCustomer)
        .toList();
  }

  @Override
  public boolean isEmpty() {
    return !mongo.exists(new Query(), CustomerDocument.class);
  }
}
