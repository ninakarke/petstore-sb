package com.petstore.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petstore.customer.internal.InMemoryCustomerStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rules for customer accounts.
 *
 * <p>Legacy sources: {@code apps/petstore/.../web/actions/CustomerHTMLAction.java} (which fields
 * are read, trimmed and required), {@code .../ejb/actions/CustomerEJBAction.java}, {@code
 * components/customer/.../ejb/CustomerEJB.java} and {@code ProfileLocalHome.java} (defaults).
 */
class CustomerServiceTests {

  private final InMemoryCustomerStore store = new InMemoryCustomerStore();
  private final CustomerService customers = new CustomerService(store);

  @Test
  @DisplayName("legacy: a new customer's account is active")
  void newCustomerHasAnActiveAccount() {
    Customer customer = customers.create("duke", CustomerFixtures.details());

    assertThat(customer.account().status()).isEqualTo("active");
    assertThat(customers.find("duke")).contains(customer);
    assertThat(customers.exists("duke")).isTrue();
  }

  @Test
  void userWithoutACustomerHasNone() {
    assertThat(customers.find("nobody")).isEmpty();
    assertThat(customers.exists("nobody")).isFalse();
    assertThat(customers.find(null)).isEmpty();
  }

  @Test
  @DisplayName("legacy: a user has at most one customer")
  void secondCustomerForTheSameUserIsRefused() {
    customers.create("duke", CustomerFixtures.details());

    assertThatThrownBy(() -> customers.create("duke", CustomerFixtures.details()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("legacy: updating replaces contact details, card and profile")
  void updateReplacesTheDetails() {
    customers.create("duke", CustomerFixtures.details());
    CustomerDetails changed =
        new CustomerDetails(
            new ContactInfo(
                "Duke",
                "BluesPrints",
                new Address("1234 Moon Way", null, "Sunville", "Texas", "10001", "Canada"),
                "duke@example.com",
                "408-555-5555"),
            new CreditCard("0100-001-0001", "Duke Express", "10/2031"),
            new Profile("en_US", "DOGS", false, true));

    Customer updated = customers.update("duke", changed);

    assertThat(updated.account().contactInfo()).isEqualTo(changed.contactInfo());
    assertThat(updated.account().creditCard()).isEqualTo(changed.creditCard());
    assertThat(updated.profile()).isEqualTo(changed.profile());
    assertThat(updated.account().status()).isEqualTo("active");
    assertThat(customers.find("duke")).contains(updated);
  }

  @Test
  void updatingACustomerThatDoesNotExistIsRefused() {
    assertThatThrownBy(() -> customers.update("nobody", CustomerFixtures.details()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("legacy: every value is trimmed; a blank second address line becomes no value")
  void valuesAreTrimmed() {
    CustomerDetails padded =
        new CustomerDetails(
            new ContactInfo(
                "  Duke ",
                " BluesPrints  ",
                new Address(" 1234 Moon Way ", "   ", " Sunville ", " Texas ", " 10001 ", " USA "),
                "  ",
                " 408-555-5555 "),
            new CreditCard(" 0100-001-0001 ", " Duke Express ", " 10/2031 "),
            new Profile(" en_US ", " DOGS ", true, true));

    Customer customer = customers.create("duke", padded);

    ContactInfo contact = customer.account().contactInfo();
    assertThat(contact.givenName()).isEqualTo("Duke");
    assertThat(contact.familyName()).isEqualTo("BluesPrints");
    assertThat(contact.address().streetName1()).isEqualTo("1234 Moon Way");
    assertThat(contact.address().streetName2()).isNull();
    assertThat(contact.address().country()).isEqualTo("USA");
    assertThat(contact.email()).isNull();
    assertThat(contact.telephone()).isEqualTo("408-555-5555");
    assertThat(customer.account().creditCard().cardNumber()).isEqualTo("0100-001-0001");
    assertThat(customer.profile().preferredLanguage()).isEqualTo("en_US");
  }

  @Test
  @DisplayName("legacy required fields: names, street, city, state, postal code, telephone")
  void requiredContactFieldsAreEnforced() {
    CustomerDetails blank =
        new CustomerDetails(
            new ContactInfo("", " ", new Address("", null, "", "", "", null), null, ""),
            CustomerFixtures.creditCard(),
            CustomerFixtures.profile());

    assertThatThrownBy(() -> customers.create("duke", blank))
        .isInstanceOfSatisfying(
            InvalidCustomerException.class,
            invalid ->
                assertThat(invalid.fields())
                    .containsExactlyInAnyOrder(
                        CustomerField.GIVEN_NAME,
                        CustomerField.FAMILY_NAME,
                        CustomerField.STREET_ADDRESS,
                        CustomerField.CITY,
                        CustomerField.STATE,
                        CustomerField.POSTAL_CODE,
                        CustomerField.TELEPHONE));
    assertThat(store.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("legacy: second address line, country and e-mail are optional")
  void optionalContactFieldsMayBeMissing() {
    CustomerDetails minimal =
        new CustomerDetails(
            new ContactInfo(
                "Duke", "B", new Address("Street", null, "City", "Texas", "1", null), null, "1"),
            CustomerFixtures.creditCard(),
            CustomerFixtures.profile());

    assertThat(customers.create("duke", minimal)).isNotNull();
  }

  @Test
  @DisplayName("fixed: card and profile fields are really required (legacy checks had no effect)")
  void cardAndProfileFieldsAreEnforced() {
    CustomerDetails incomplete =
        new CustomerDetails(
            CustomerFixtures.contactInfo(),
            new CreditCard("", "", "/"),
            new Profile("", "", true, true));

    assertThatThrownBy(() -> customers.create("duke", incomplete))
        .isInstanceOfSatisfying(
            InvalidCustomerException.class,
            invalid ->
                assertThat(invalid.fields())
                    .containsExactlyInAnyOrder(
                        CustomerField.CARD_NUMBER,
                        CustomerField.CARD_TYPE,
                        CustomerField.CARD_EXPIRY,
                        CustomerField.FAVORITE_CATEGORY));
  }

  @Test
  @DisplayName("the store is English only: a profile without a language gets en_US")
  void profileWithoutALanguageGetsEnglish() {
    CustomerDetails noLanguage =
        new CustomerDetails(
            CustomerFixtures.contactInfo(),
            CustomerFixtures.creditCard(),
            new Profile("", "DOGS", true, true));

    assertThat(customers.create("duke", noLanguage).profile().preferredLanguage())
        .isEqualTo("en_US");
  }

  @Test
  void valuesThatWouldNotFitAreRefused() {
    CustomerDetails huge =
        new CustomerDetails(
            new ContactInfo(
                "x".repeat(256),
                "B",
                new Address("Street", "y".repeat(256), "City", "Texas", "1", null),
                "e".repeat(256),
                "1"),
            CustomerFixtures.creditCard(),
            CustomerFixtures.profile());

    assertThatThrownBy(() -> customers.create("duke", huge))
        .isInstanceOfSatisfying(
            InvalidCustomerException.class,
            invalid ->
                assertThat(invalid.fields())
                    .containsExactlyInAnyOrder(
                        CustomerField.GIVEN_NAME,
                        CustomerField.STREET_ADDRESS,
                        CustomerField.EMAIL));
  }
}
