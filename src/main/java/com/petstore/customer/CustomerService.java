package com.petstore.customer;

import com.petstore.customer.internal.CustomerStore;
import com.petstore.shared.StoreLocale;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Customer accounts: creating one, reading it and changing it.
 *
 * <p>Replaces the legacy {@code CustomerEJBAction}, the customer methods of the shopping client
 * facade, and the creation logic of {@code CustomerEJB.ejbPostCreate}.
 */
@Service
public class CustomerService {

  private static final int MAX_LENGTH = 255;

  private final CustomerStore customers;

  public CustomerService(CustomerStore customers) {
    this.customers = customers;
  }

  public Optional<Customer> find(String userId) {
    return userId == null ? Optional.empty() : customers.find(userId);
  }

  public boolean exists(String userId) {
    return find(userId).isPresent();
  }

  /**
   * Creates the customer of a user. The new account is active, as in the legacy application.
   *
   * @throws InvalidCustomerException if required details are missing
   * @throws IllegalStateException if the user already has a customer
   */
  public Customer create(String userId, CustomerDetails details) {
    if (exists(userId)) {
      throw new IllegalStateException("User " + userId + " already has a customer");
    }
    return store(userId, Account.ACTIVE, details);
  }

  /**
   * Changes the details of an existing customer. The account status is left as it is.
   *
   * @throws InvalidCustomerException if required details are missing
   * @throws IllegalStateException if the user has no customer
   */
  public Customer update(String userId, CustomerDetails details) {
    Customer existing =
        find(userId)
            .orElseThrow(() -> new IllegalStateException("User " + userId + " has no customer"));
    return store(userId, existing.account().status(), details);
  }

  private Customer store(String userId, String status, CustomerDetails details) {
    CustomerDetails clean = normalise(details);
    validate(clean);
    Customer customer =
        new Customer(
            userId, new Account(status, clean.contactInfo(), clean.creditCard()), clean.profile());
    customers.save(customer);
    return customer;
  }

  /**
   * Trims every value, as the legacy form handler did, and turns blank optional values into "no
   * value".
   */
  private static CustomerDetails normalise(CustomerDetails details) {
    ContactInfo contact = details.contactInfo();
    Address address = contact.address();
    CreditCard card = details.creditCard();
    Profile profile = details.profile();
    return new CustomerDetails(
        new ContactInfo(
            trim(contact.givenName()),
            trim(contact.familyName()),
            new Address(
                trim(address.streetName1()),
                blankToNull(address.streetName2()),
                trim(address.city()),
                trim(address.state()),
                trim(address.zipCode()),
                blankToNull(address.country())),
            blankToNull(contact.email()),
            trim(contact.telephone())),
        new CreditCard(trim(card.cardNumber()), trim(card.cardType()), trim(card.expiryDate())),
        new Profile(
            language(profile.preferredLanguage()),
            trim(profile.favoriteCategory()),
            profile.myListPreference(),
            profile.bannerPreference()));
  }

  /**
   * Required fields. The legacy form handler listed the same ones (first name, last name, street
   * address, city, state, postal code, telephone; second address line, country and e-mail are
   * optional) but only checked them in the browser, and its checks of the card and profile fields
   * had no effect. Here they are enforced on the server.
   */
  private static void validate(CustomerDetails details) {
    EnumSet<CustomerField> problems = EnumSet.noneOf(CustomerField.class);
    ContactInfo contact = details.contactInfo();
    Address address = contact.address();
    CreditCard card = details.creditCard();
    Profile profile = details.profile();

    require(contact.givenName(), CustomerField.GIVEN_NAME, problems);
    require(contact.familyName(), CustomerField.FAMILY_NAME, problems);
    require(address.streetName1(), CustomerField.STREET_ADDRESS, problems);
    require(address.city(), CustomerField.CITY, problems);
    require(address.state(), CustomerField.STATE, problems);
    require(address.zipCode(), CustomerField.POSTAL_CODE, problems);
    require(contact.telephone(), CustomerField.TELEPHONE, problems);
    require(card.cardNumber(), CustomerField.CARD_NUMBER, problems);
    require(card.cardType(), CustomerField.CARD_TYPE, problems);
    if (card.expiryMonth().isBlank() || card.expiryYear().isBlank()) {
      problems.add(CustomerField.CARD_EXPIRY);
    }
    require(profile.favoriteCategory(), CustomerField.FAVORITE_CATEGORY, problems);
    tooLong(address.streetName2(), CustomerField.STREET_ADDRESS, problems);
    tooLong(contact.email(), CustomerField.EMAIL, problems);

    if (!problems.isEmpty()) {
      throw new InvalidCustomerException(problems);
    }
  }

  private static void require(String value, CustomerField field, EnumSet<CustomerField> problems) {
    if (value == null || value.isBlank()) {
      problems.add(field);
    }
    tooLong(value, field, problems);
  }

  private static void tooLong(String value, CustomerField field, EnumSet<CustomerField> problems) {
    if (value != null && value.length() > MAX_LENGTH) {
      problems.add(field);
    }
  }

  /**
   * The store is English only, so a profile without a language gets the English code. A value that
   * is already stored, such as on the seed customers, is left as it is; nothing reads it.
   */
  private static String language(String value) {
    return value == null || value.isBlank() ? StoreLocale.CODE : value.trim();
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
