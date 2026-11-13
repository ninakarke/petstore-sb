package com.petstore.customer;

/**
 * A postal address.
 *
 * <p>Legacy equivalent: the {@code Address} entity bean and value class.
 */
public record Address(
    String streetName1,
    String streetName2,
    String city,
    String state,
    String zipCode,
    String country) {}
