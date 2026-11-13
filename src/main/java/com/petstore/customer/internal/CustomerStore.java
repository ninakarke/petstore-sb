package com.petstore.customer.internal;

import com.petstore.customer.Customer;
import java.util.Optional;

/**
 * The persistence port of the customer module.
 *
 * <p>A customer is read and written as one aggregate. In the legacy application the same data was
 * six entity beans with their own home interfaces, navigated one relationship at a time. Every
 * adapter must pass the same contract test suite.
 */
public interface CustomerStore {

  Optional<Customer> find(String userId);

  /** Stores the customer, replacing any customer with the same user id. */
  void save(Customer customer);

  boolean isEmpty();
}
