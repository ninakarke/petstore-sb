package com.petstore.customer.internal.web;

import com.petstore.customer.AddressChoices;
import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The choices offered by the drop-down lists of the account form.
 *
 * <p>The lists are the ones of the legacy {@code create_customer.jsp} and {@code
 * edit_customer.jsp}, with two repairs. The legacy pages offered expiry years 2001 to 2005, fixed
 * in the page; here the list starts at the current year. And where the legacy edit page silently
 * showed the first choice when the stored value was not among the choices, the stored value is
 * added to the list, so saving the form without touching a field does not change it.
 */
record AccountFormOptions(
    List<String> states,
    List<String> countries,
    List<String> cardTypes,
    List<String> months,
    List<String> years) {

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
        AddressChoices.states(form.state()),
        AddressChoices.countries(form.country()),
        AddressChoices.including(CARD_TYPES, form.cardType()),
        AddressChoices.including(months, form.expiryMonth()),
        AddressChoices.including(years, form.expiryYear()));
  }
}
