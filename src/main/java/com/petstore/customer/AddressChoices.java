package com.petstore.customer;

import java.util.ArrayList;
import java.util.List;

/**
 * The states and countries offered wherever an address is entered: on the account form and on the
 * order form.
 *
 * <p>The lists are those of the legacy pages. A value that is already stored but is not on the list
 * is offered as well, so that showing a form and saving it does not change an address the visitor
 * did not touch (the legacy seed customers have state "CA", the list says "California").
 */
public final class AddressChoices {

  private static final List<String> STATES = List.of("California", "New York", "Texas");
  private static final List<String> COUNTRIES = List.of("USA", "Canada", "Japan", "China");

  private AddressChoices() {}

  public static List<String> states(String... current) {
    return including(STATES, current);
  }

  public static List<String> countries(String... current) {
    return including(COUNTRIES, current);
  }

  /** The choices, preceded by any current value that is not one of them. */
  public static List<String> including(List<String> choices, String... current) {
    List<String> extended = new ArrayList<>();
    for (String value : current) {
      if (value != null
          && !value.isBlank()
          && !choices.contains(value)
          && !extended.contains(value)) {
        extended.add(value);
      }
    }
    extended.addAll(choices);
    return List.copyOf(extended);
  }
}
