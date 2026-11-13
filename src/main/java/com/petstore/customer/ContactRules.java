package com.petstore.customer;

import java.util.EnumSet;

/**
 * The rules for contact details, wherever they are entered: on the account form, on the order form
 * and in the JSON API.
 *
 * <p>Legacy source: {@code extractContactInfo} in {@code CustomerHTMLAction} and {@code
 * OrderHTMLAction}, two copies of the same code. Values are trimmed. First name, last name, street
 * address, city, state, postal code and telephone are required; the second address line, the
 * country and the e-mail address are optional. The legacy application only checked this in the
 * browser.
 */
public final class ContactRules {

  private static final int MAX_LENGTH = 255;

  private ContactRules() {}

  /** Trims every value and turns blank optional values into "no value". */
  public static ContactInfo normalise(ContactInfo contact) {
    Address address = contact.address();
    return new ContactInfo(
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
        trim(contact.telephone()));
  }

  /** The fields of already normalised contact details that are missing or too long. */
  public static EnumSet<CustomerField> problems(ContactInfo contact) {
    EnumSet<CustomerField> problems = EnumSet.noneOf(CustomerField.class);
    Address address = contact.address();
    require(contact.givenName(), CustomerField.GIVEN_NAME, problems);
    require(contact.familyName(), CustomerField.FAMILY_NAME, problems);
    require(address.streetName1(), CustomerField.STREET_ADDRESS, problems);
    require(address.city(), CustomerField.CITY, problems);
    require(address.state(), CustomerField.STATE, problems);
    require(address.zipCode(), CustomerField.POSTAL_CODE, problems);
    require(contact.telephone(), CustomerField.TELEPHONE, problems);
    tooLong(address.streetName2(), CustomerField.STREET_ADDRESS, problems);
    tooLong(contact.email(), CustomerField.EMAIL, problems);
    return problems;
  }

  static void require(String value, CustomerField field, EnumSet<CustomerField> problems) {
    if (value == null || value.isBlank()) {
      problems.add(field);
    }
    tooLong(value, field, problems);
  }

  static void tooLong(String value, CustomerField field, EnumSet<CustomerField> problems) {
    if (value != null && value.length() > MAX_LENGTH) {
      problems.add(field);
    }
  }

  static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
