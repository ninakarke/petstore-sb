package com.petstore.customer;

/**
 * A customer: one aggregate made of the account (contact details and payment card) and the profile
 * (preferences).
 *
 * <p>Legacy equivalent: six entity beans, {@code Customer}, {@code Account}, {@code ContactInfo},
 * {@code Address}, {@code CreditCard} and {@code Profile}, linked by one-to-one relationships with
 * cascading delete. They were always created, read and removed together, which is what makes them
 * one aggregate.
 *
 * @param userId the id of the user this customer belongs to
 */
public record Customer(String userId, Account account, Profile profile) {}
