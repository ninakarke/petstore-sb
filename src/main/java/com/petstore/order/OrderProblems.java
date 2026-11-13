package com.petstore.order;

import com.petstore.customer.CustomerField;
import java.util.List;
import java.util.Set;

/**
 * Everything that is wrong with a requested order, collected so that it can be reported at once.
 *
 * @param billTo billing contact fields that are missing or unusable
 * @param shipTo shipping contact fields that are missing or unusable
 * @param unknownItems requested item ids that are not in the catalog
 * @param invalidQuantities item ids requested with a quantity that is not allowed
 * @param noLines whether the order has no lines at all
 */
public record OrderProblems(
    Set<CustomerField> billTo,
    Set<CustomerField> shipTo,
    List<String> unknownItems,
    List<String> invalidQuantities,
    boolean noLines) {

  public OrderProblems {
    billTo = Set.copyOf(billTo);
    shipTo = Set.copyOf(shipTo);
    unknownItems = List.copyOf(unknownItems);
    invalidQuantities = List.copyOf(invalidQuantities);
  }

  public boolean isEmpty() {
    return billTo.isEmpty()
        && shipTo.isEmpty()
        && unknownItems.isEmpty()
        && invalidQuantities.isEmpty()
        && !noLines;
  }
}
