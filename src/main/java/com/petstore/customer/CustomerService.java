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

  /** Trims every value, as the legacy form handler did. */
  private static CustomerDetails normalise(CustomerDetails details) {
    CreditCard card = details.creditCard();
    Profile profile = details.profile();
    return new CustomerDetails(
        ContactRules.normalise(details.contactInfo()),
        new CreditCard(
            ContactRules.trim(card.cardNumber()),
            ContactRules.trim(card.cardType()),
            ContactRules.trim(card.expiryDate())),
        new Profile(
            language(profile.preferredLanguage()),
            ContactRules.trim(profile.favoriteCategory()),
            profile.myListPreference(),
            profile.bannerPreference()));
  }

  /**
   * Required fields: those of {@link ContactRules}, plus card number, card type, expiry date and
   * favourite category. The legacy form handler's checks of the card and profile fields had no
   * effect; here they are enforced on the server.
   */
  private static void validate(CustomerDetails details) {
    EnumSet<CustomerField> problems = ContactRules.problems(details.contactInfo());
    CreditCard card = details.creditCard();
    ContactRules.require(card.cardNumber(), CustomerField.CARD_NUMBER, problems);
    ContactRules.require(card.cardType(), CustomerField.CARD_TYPE, problems);
    if (card.expiryMonth().isBlank() || card.expiryYear().isBlank()) {
      problems.add(CustomerField.CARD_EXPIRY);
    }
    ContactRules.require(
        details.profile().favoriteCategory(), CustomerField.FAVORITE_CATEGORY, problems);
    if (!problems.isEmpty()) {
      throw new InvalidCustomerException(problems);
    }
  }

  /**
   * The store is English only, so a profile without a language gets the English code. A value that
   * is already stored, such as on the seed customers, is left as it is; nothing reads it.
   */
  private static String language(String value) {
    return value == null || value.isBlank() ? StoreLocale.CODE : value.trim();
  }
}
