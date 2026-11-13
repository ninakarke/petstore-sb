package com.petstore.customer.internal.persistence.mongo;

import com.petstore.customer.Account;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A customer, one document in {@code customers}, with everything that belongs to the customer
 * inside it.
 *
 * <p>In the relational schema the same data is six tables that share the customer's key, and in the
 * legacy application it was six entity beans. A customer is always read and saved whole, and
 * nothing in it is shared with another customer, so here it is one document: one read, one write,
 * and no transaction needed to keep its parts consistent.
 *
 * <p>These records are the schema of the collection. They are deliberately separate from the domain
 * classes, so that renaming something in the domain cannot silently rename a stored field.
 */
@Document("customers")
record CustomerDocument(@Id String userId, AccountPart account, ProfilePart profile) {

  record AccountPart(String status, ContactPart contact, CardPart creditCard) {}

  record ContactPart(
      String givenName, String familyName, String email, String telephone, AddressPart address) {}

  record AddressPart(
      String street1,
      String street2,
      String city,
      String state,
      String postalCode,
      String country) {}

  record CardPart(String number, String type, String expiry) {}

  record ProfilePart(
      String preferredLanguage, String favoriteCategory, boolean myList, boolean banner) {}

  static CustomerDocument of(Customer customer) {
    ContactInfo contact = customer.account().contactInfo();
    Address address = contact.address();
    CreditCard card = customer.account().creditCard();
    Profile profile = customer.profile();
    return new CustomerDocument(
        customer.userId(),
        new AccountPart(
            customer.account().status(),
            new ContactPart(
                contact.givenName(),
                contact.familyName(),
                contact.email(),
                contact.telephone(),
                new AddressPart(
                    address.streetName1(),
                    address.streetName2(),
                    address.city(),
                    address.state(),
                    address.zipCode(),
                    address.country())),
            new CardPart(card.cardNumber(), card.cardType(), card.expiryDate())),
        new ProfilePart(
            profile.preferredLanguage(),
            profile.favoriteCategory(),
            profile.myListPreference(),
            profile.bannerPreference()));
  }

  Customer toCustomer() {
    ContactPart contact = account.contact();
    AddressPart address = contact.address();
    CardPart card = account.creditCard();
    return new Customer(
        userId,
        new Account(
            account.status(),
            new ContactInfo(
                contact.givenName(),
                contact.familyName(),
                new Address(
                    address.street1(),
                    address.street2(),
                    address.city(),
                    address.state(),
                    address.postalCode(),
                    address.country()),
                contact.email(),
                contact.telephone()),
            new CreditCard(card.number(), card.type(), card.expiry())),
        new Profile(
            profile.preferredLanguage(),
            profile.favoriteCategory(),
            profile.myList(),
            profile.banner()));
  }
}
