package com.petstore.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Small rules of the customer model.
 *
 * <p>Legacy sources: {@code components/creditcard/.../ejb/CreditCardEJB.java} ({@code
 * getExpiryMonth}, {@code getExpiryYear}) and {@code
 * components/customer/.../profile/ejb/ProfileLocalHome.java}.
 */
class CustomerModelTests {

  @Test
  @DisplayName("legacy: the expiry date is month/year text and is split at the slash")
  void expiryDateIsSplitAtTheSlash() {
    CreditCard card = CreditCard.of("1", "Meow Card", "04", "2030");

    assertThat(card.expiryDate()).isEqualTo("04/2030");
    assertThat(card.expiryMonth()).isEqualTo("04");
    assertThat(card.expiryYear()).isEqualTo("2030");
  }

  @Test
  @DisplayName("legacy: an expiry date without a slash reads as 01 / 2010")
  void expiryDateWithoutSlashHasDefaults() {
    assertThat(new CreditCard("1", "x", "garbage").expiryMonth()).isEqualTo("01");
    assertThat(new CreditCard("1", "x", "garbage").expiryYear()).isEqualTo("2010");
    assertThat(new CreditCard("1", "x", null).expiryMonth()).isEqualTo("01");
    assertThat(new CreditCard("1", "x", null).expiryYear()).isEqualTo("2010");
  }

  @Test
  @DisplayName("fixed: only the last four characters of a card number are shown")
  void cardNumberIsMasked() {
    assertThat(new CreditCard("0100-001-0001", "x", "1/1").maskedNumber())
        .isEqualTo("*********0001");
    assertThat(new CreditCard("123456789", "x", "1/1").maskedNumber()).isEqualTo("*****6789");
    assertThat(new CreditCard("12345", "x", "1/1").maskedNumber()).isEqualTo("****5");
    assertThat(new CreditCard("1234", "x", "1/1").maskedNumber()).isEqualTo("****");
    assertThat(new CreditCard("", "x", "1/1").maskedNumber()).isEmpty();
    assertThat(new CreditCard(null, "x", "1/1").maskedNumber()).isEmpty();
  }

  @Test
  @DisplayName("legacy: profile defaults are en_US, no favourite category, my list on, banner on")
  void profileDefaults() {
    assertThat(Profile.defaults()).isEqualTo(new Profile("en_US", null, true, true));
  }
}
