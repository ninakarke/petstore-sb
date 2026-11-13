package com.petstore.customer;

/**
 * How to reach a person: name, address, e-mail and telephone.
 *
 * <p>Legacy equivalent: the {@code ContactInfo} entity bean and value class.
 */
public record ContactInfo(
    String givenName, String familyName, Address address, String email, String telephone) {}
