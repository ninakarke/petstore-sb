package com.petstore.customer;

/**
 * A customer's account: its status, the contact details and the payment card.
 *
 * <p>Legacy equivalent: the {@code Account} entity bean.
 */
public record Account(String status, ContactInfo contactInfo, CreditCard creditCard) {

  /** Legacy: {@code AccountLocalHome.Active}. */
  public static final String ACTIVE = "active";

  /** Legacy: {@code AccountLocalHome.Disabled}. */
  public static final String DISABLED = "disabled";
}
