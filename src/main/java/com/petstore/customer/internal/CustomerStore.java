package com.petstore.customer.internal;

import com.petstore.customer.Customer;
import java.util.List;
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

  /**
   * Customers in the store's key order: at most {@code limit} of them, starting with the customer
   * of the given user id, or with the first one if the id is {@code null}. If there is no such
   * customer, the list starts with the next one in key order. Used to copy customers to another
   * store.
   */
  List<Customer> customersFrom(String startUserId, int limit);
}
