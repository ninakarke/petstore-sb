package com.petstore.order.internal.persistence.jpa;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import jakarta.persistence.Embeddable;

/**
 * Contact details as ten columns of the order row. Used twice per order, for the shipping and the
 * billing contact, with a column prefix each.
 */
@Embeddable
class ContactEmbeddable {

  String givenName;
  String familyName;
  String streetName1;
  String streetName2;
  String city;
  String state;
  String zipCode;
  String country;
  String email;
  String telephone;

  protected ContactEmbeddable() {}

  ContactEmbeddable(ContactInfo contact) {
    Address address = contact.address();
    this.givenName = contact.givenName();
    this.familyName = contact.familyName();
    this.streetName1 = address.streetName1();
    this.streetName2 = address.streetName2();
    this.city = address.city();
    this.state = address.state();
    this.zipCode = address.zipCode();
    this.country = address.country();
    this.email = contact.email();
    this.telephone = contact.telephone();
  }

  ContactInfo toContactInfo() {
    return new ContactInfo(
        givenName,
        familyName,
        new Address(streetName1, streetName2, city, state, zipCode, country),
        email,
        telephone);
  }
}
