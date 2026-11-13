package com.petstore.customer.internal.web;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerDetails;
import com.petstore.customer.Profile;
import java.util.Map;

/**
 * The fields of the account form, as text, exactly as the visitor entered or will see them.
 *
 * <p>Legacy equivalent: the request parameters read by {@code CustomerHTMLAction} ({@code
 * given_name_a}, {@code credit_card_expiry_month}, {@code mylist_on}, ...). The legacy form also
 * asked for a language; the store is English only now, so that field is gone.
 *
 * @param cardNumber the card number as typed. On the edit form it starts empty, so the stored
 *     number is never sent back to the browser; leaving it empty keeps the stored number.
 */
record AccountForm(
    String givenName,
    String familyName,
    String address1,
    String address2,
    String city,
    String state,
    String postalCode,
    String country,
    String telephone,
    String email,
    String cardNumber,
    String cardType,
    String expiryMonth,
    String expiryYear,
    String favoriteCategory,
    boolean myList,
    boolean banners) {

  /** An empty form for a new customer, with the legacy profile defaults. */
  static AccountForm empty() {
    Profile defaults = Profile.defaults();
    return new AccountForm(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        defaults.myListPreference(),
        defaults.bannerPreference());
  }

  /** The form showing an existing customer. The card number is deliberately left out. */
  static AccountForm of(Customer customer) {
    ContactInfo contact = customer.account().contactInfo();
    Address address = contact.address();
    CreditCard card = customer.account().creditCard();
    Profile profile = customer.profile();
    return new AccountForm(
        text(contact.givenName()),
        text(contact.familyName()),
        text(address.streetName1()),
        text(address.streetName2()),
        text(address.city()),
        text(address.state()),
        text(address.zipCode()),
        text(address.country()),
        text(contact.telephone()),
        text(contact.email()),
        "",
        text(card.cardType()),
        card.expiryMonth(),
        card.expiryYear(),
        text(profile.favoriteCategory()),
        profile.myListPreference(),
        profile.bannerPreference());
  }

  /** The form as submitted. A checkbox that is not ticked is simply absent from the request. */
  static AccountForm submitted(Map<String, String> parameters) {
    return new AccountForm(
        value(parameters, "givenName"),
        value(parameters, "familyName"),
        value(parameters, "address1"),
        value(parameters, "address2"),
        value(parameters, "city"),
        value(parameters, "state"),
        value(parameters, "postalCode"),
        value(parameters, "country"),
        value(parameters, "telephone"),
        value(parameters, "email"),
        value(parameters, "cardNumber"),
        value(parameters, "cardType"),
        value(parameters, "expiryMonth"),
        value(parameters, "expiryYear"),
        value(parameters, "favoriteCategory"),
        parameters.containsKey("myList"),
        parameters.containsKey("banners"));
  }

  /**
   * What the form means.
   *
   * @param storedCardNumber the card number already on the account, used when the field was left
   *     empty; {@code null} for a new customer
   * @param storedLanguage the language value already on the profile, carried over unchanged; {@code
   *     null} for a new customer
   */
  CustomerDetails toDetails(String storedCardNumber, String storedLanguage) {
    String number =
        cardNumber.isBlank() && storedCardNumber != null ? storedCardNumber : cardNumber;
    return new CustomerDetails(
        new ContactInfo(
            givenName,
            familyName,
            new Address(address1, address2, city, state, postalCode, country),
            email,
            telephone),
        CreditCard.of(number, cardType, expiryMonth, expiryYear),
        new Profile(storedLanguage, favoriteCategory, myList, banners));
  }

  private static String value(Map<String, String> parameters, String name) {
    return text(parameters.get(name));
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
