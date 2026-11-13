package com.petstore.customer;

/** Sample customers for tests. The values are those of the legacy seed customer {@code j2ee}. */
public final class CustomerFixtures {

  private CustomerFixtures() {}

  public static Address address() {
    return new Address("1234 Anywhere Street", "Unit 555", "Palo Alto", "CA", "94303", "USA");
  }

  public static ContactInfo contactInfo() {
    return new ContactInfo("XYZ", "ABC", address(), "aaa@bbb.ccc", "555-555-5555");
  }

  public static CreditCard creditCard() {
    return new CreditCard("123456789", "Meow Card", "04/2030");
  }

  public static Profile profile() {
    return new Profile("en_US", "REPTILES", true, false);
  }

  public static CustomerDetails details() {
    return new CustomerDetails(contactInfo(), creditCard(), profile());
  }

  public static Customer customer(String userId) {
    return new Customer(
        userId, new Account(Account.ACTIVE, contactInfo(), creditCard()), profile());
  }
}
