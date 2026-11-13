package com.petstore.order.internal.web;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import java.util.Map;

/**
 * One block of the order form, billing or shipping, as text.
 *
 * <p>Legacy equivalent: the request parameters with the suffix {@code _a} (billing) or {@code _b}
 * (shipping) read by {@code OrderHTMLAction.extractContactInfo}.
 */
record ContactForm(
    String givenName,
    String familyName,
    String address1,
    String address2,
    String city,
    String state,
    String postalCode,
    String country,
    String telephone,
    String email) {

  /** The block prefilled with a customer's contact details, as the legacy order form was. */
  static ContactForm of(ContactInfo contact) {
    Address address = contact.address();
    return new ContactForm(
        text(contact.givenName()),
        text(contact.familyName()),
        text(address.streetName1()),
        text(address.streetName2()),
        text(address.city()),
        text(address.state()),
        text(address.zipCode()),
        text(address.country()),
        text(contact.telephone()),
        text(contact.email()));
  }

  /** The block as submitted; its fields are named {@code <prefix>GivenName}, and so on. */
  static ContactForm submitted(Map<String, String> parameters, String prefix) {
    return new ContactForm(
        text(parameters.get(prefix + "GivenName")),
        text(parameters.get(prefix + "FamilyName")),
        text(parameters.get(prefix + "Address1")),
        text(parameters.get(prefix + "Address2")),
        text(parameters.get(prefix + "City")),
        text(parameters.get(prefix + "State")),
        text(parameters.get(prefix + "PostalCode")),
        text(parameters.get(prefix + "Country")),
        text(parameters.get(prefix + "Telephone")),
        text(parameters.get(prefix + "Email")));
  }

  ContactInfo toContactInfo() {
    return new ContactInfo(
        givenName,
        familyName,
        new Address(address1, address2, city, state, postalCode, country),
        email,
        telephone);
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
