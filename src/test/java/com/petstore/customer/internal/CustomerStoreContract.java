package com.petstore.customer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.customer.Account;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The contract every {@link CustomerStore} adapter must fulfil. Written once against the port and
 * run for each data store by a small subclass.
 *
 * <p>Legacy source: the entity beans of {@code components/customer}, {@code contactinfo}, {@code
 * address} and {@code creditcard}, and their relationships in {@code
 * components/customer/src/ejb-jar.xml}.
 */
public abstract class CustomerStoreContract {

  /** The adapter under test. */
  protected abstract CustomerStore store();

  /** Hook for stores that do not roll back after a test. */
  protected void emptyTheStore() {}

  @BeforeEach
  void startEmpty() {
    emptyTheStore();
  }

  @Test
  void newStoreIsEmpty() {
    assertThat(store().isEmpty()).isTrue();
    assertThat(store().find("j2ee")).isEmpty();
  }

  @Test
  @DisplayName("legacy: a customer is its account, contact details, address, card and profile")
  void savedCustomerIsReadBackComplete() {
    Customer customer = CustomerFixtures.customer("j2ee");

    store().save(customer);

    assertThat(store().find("j2ee")).contains(customer);
    assertThat(store().isEmpty()).isFalse();
  }

  @Test
  void customersAreKeptApart() {
    store().save(CustomerFixtures.customer("j2ee"));
    Customer other =
        new Customer(
            "shopper",
            new Account(
                Account.DISABLED,
                new ContactInfo(
                    "Duke",
                    "BluesPrints",
                    new Address("1234 Moon Way", null, "Sunville", "Texas", "10001", "Canada"),
                    null,
                    "408-555-5555"),
                new CreditCard("0100-001-0001", "Duke Express", "10/2031")),
            new Profile("ja_JP", "DOGS", false, true));

    store().save(other);

    assertThat(store().find("shopper")).contains(other);
    assertThat(store().find("j2ee")).contains(CustomerFixtures.customer("j2ee"));
  }

  @Test
  void savingAgainReplacesTheWholeCustomer() {
    store().save(CustomerFixtures.customer("j2ee"));
    Customer changed =
        new Customer(
            "j2ee",
            new Account(
                Account.ACTIVE,
                new ContactInfo(
                    "New",
                    "Name",
                    new Address("1 New Road", null, "Newtown", "New York", "10002", "USA"),
                    "new@example.com",
                    "111-111-1111"),
                new CreditCard("999", "Java(TM) Card", "01/2032")),
            new Profile("zh_CN", "CATS", false, false));

    store().save(changed);

    assertThat(store().find("j2ee")).contains(changed);
  }

  @Test
  @DisplayName("legacy: second address line, country, e-mail and favourite category are optional")
  void optionalValuesMayBeAbsent() {
    Customer sparse =
        new Customer(
            "sparse",
            new Account(
                Account.ACTIVE,
                new ContactInfo(
                    "A", "B", new Address("Street", null, "City", "CA", "1", null), null, "1"),
                new CreditCard("1", "Meow Card", "01/2030")),
            new Profile("en_US", null, true, true));

    store().save(sparse);

    assertThat(store().find("sparse")).contains(sparse);
  }

  @Test
  void textInOtherScriptsIsKept() {
    Customer customer =
        new Customer(
            "j2ee-zh",
            new Account(
                Account.ACTIVE,
                new ContactInfo(
                    "小明",
                    "王",
                    new Address("南京路 1 号", null, "苏州", "江苏省", "215000", "中国"),
                    "x@y.z",
                    "555"),
                new CreditCard("1", "VISA", "01/2030")),
            new Profile("zh_CN", "FISH", true, true));

    store().save(customer);

    assertThat(store().find("j2ee-zh")).contains(customer);
  }
}
