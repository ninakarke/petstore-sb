package com.petstore.customer;

/** What a customer enters on the account form. */
public record CustomerDetails(ContactInfo contactInfo, CreditCard creditCard, Profile profile) {}
