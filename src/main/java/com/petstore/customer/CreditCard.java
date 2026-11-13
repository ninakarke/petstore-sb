package com.petstore.customer;

/**
 * A payment card.
 *
 * <p>Legacy equivalent: the {@code CreditCard} entity bean and value class. As there, the expiry
 * date is kept as text of the form {@code month/year}.
 */
public record CreditCard(String cardNumber, String cardType, String expiryDate) {

  /** Builds a card from the separate month and year fields of the account form. */
  public static CreditCard of(
      String cardNumber, String cardType, String expiryMonth, String expiryYear) {
    return new CreditCard(cardNumber, cardType, expiryMonth + "/" + expiryYear);
  }

  /** The part before the slash; {@code 01} if there is none. Legacy: {@code getExpiryMonth}. */
  public String expiryMonth() {
    int slash = expiryDate == null ? -1 : expiryDate.indexOf('/');
    return slash == -1 ? "01" : expiryDate.substring(0, slash);
  }

  /** The part after the slash; {@code 2010} if there is none. Legacy: {@code getExpiryYear}. */
  public String expiryYear() {
    int slash = expiryDate == null ? -1 : expiryDate.indexOf('/');
    return slash == -1 ? "2010" : expiryDate.substring(slash + 1);
  }

  /**
   * The card number with everything but the last four characters hidden. The legacy account page
   * displayed the full number.
   */
  public String maskedNumber() {
    if (cardNumber == null) {
      return "";
    }
    int visible = Math.min(4, Math.max(cardNumber.length() - 4, 0));
    int hidden = cardNumber.length() - visible;
    return "*".repeat(hidden) + cardNumber.substring(hidden);
  }
}
