package com.petstore.customer.internal;

import com.petstore.customer.Customer;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreCopy;
import java.util.List;

/** Copies all customers from one store to another, through the customer port on both sides. */
public final class CustomerCopy {

  private CustomerCopy() {}

  public static List<CopyReport> copy(CustomerStore source, CustomerStore target, int batchSize) {
    return List.of(
        StoreCopy.copy(
            "customers",
            Customer::userId,
            source::customersFrom,
            target::customersFrom,
            target::save,
            batchSize));
  }
}
