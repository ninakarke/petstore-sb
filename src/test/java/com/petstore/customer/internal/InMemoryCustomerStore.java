package com.petstore.customer.internal;

import com.petstore.customer.Customer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A customer store that keeps everything in memory, for fast tests of the logic above the port.
 * Held to the same {@link CustomerStoreContract} as the real adapters.
 */
public class InMemoryCustomerStore implements CustomerStore {

  private final Map<String, Customer> customers = new LinkedHashMap<>();

  @Override
  public Optional<Customer> find(String userId) {
    return Optional.ofNullable(customers.get(userId));
  }

  @Override
  public void save(Customer customer) {
    customers.put(customer.userId(), customer);
  }

  @Override
  public boolean isEmpty() {
    return customers.isEmpty();
  }

  public int size() {
    return customers.size();
  }
}
