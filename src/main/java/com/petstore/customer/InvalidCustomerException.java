package com.petstore.customer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Thrown when account details are incomplete or unusable. Names the fields concerned. */
public class InvalidCustomerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final EnumSet<CustomerField> fields;

  public InvalidCustomerException(Set<CustomerField> fields) {
    super("Invalid customer details: " + fields);
    this.fields = EnumSet.copyOf(fields);
  }

  public Set<CustomerField> fields() {
    return Collections.unmodifiableSet(fields);
  }
}
