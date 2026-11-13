package com.petstore.customer.internal.web;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The choices offered by the drop-down lists of the account form.
 *
 * <p>The lists are the ones of the legacy {@code create_customer.jsp} and {@code
 * edit_customer.jsp}, with two repairs. The legacy pages offered expiry years 2001 to 2005, fixed
 * in the page; here the list starts at the current year. And where the legacy edit page silently
 * showed the first choice when the stored value was not among the choices (the seed customers have
 * state "CA" while the list says "California"), the stored value is added to the list, so saving
 * the form without touching a field does not change it.
 */
record AccountFormOptions(
    List<String> states,
    List<String> countries,
    List<String> cardTypes,
    List<String> months,
    List<String> years) {

  private static final List<String> STATES = List.of("California", "New York", "Texas");
  private static final List<String> COUNTRIES = List.of("USA", "Canada", "Japan", "China");
  private static final List<String> CARD_TYPES =
      List.of("Java(TM) Card", "Duke Express", "Meow Card");
  private static final int YEARS_OFFERED = 10;

  static AccountFormOptions forForm(AccountForm form, Year currentYear) {
    List<String> months =
        IntStream.rangeClosed(1, 12).mapToObj(month -> "%02d".formatted(month)).toList();
    List<String> years =
        IntStream.rangeClosed(0, YEARS_OFFERED)
            .mapToObj(offset -> String.valueOf(currentYear.getValue() + offset))
            .toList();
    return new AccountFormOptions(
        including(STATES, form.state()),
        including(COUNTRIES, form.country()),
        including(CARD_TYPES, form.cardType()),
        including(months, form.expiryMonth()),
        including(years, form.expiryYear()));
  }

  /** The choices, preceded by the current value if it is not one of them. */
  private static List<String> including(List<String> choices, String current) {
    if (current == null || current.isBlank() || choices.contains(current)) {
      return choices;
    }
    List<String> extended = new ArrayList<>();
    extended.add(current);
    extended.addAll(choices);
    return List.copyOf(extended);
  }
}
