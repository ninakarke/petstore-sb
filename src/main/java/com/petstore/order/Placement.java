package com.petstore.order;

/**
 * The outcome of placing an order through the JSON API.
 *
 * @param order the order
 * @param replayed {@code true} if the request repeated an earlier one with the same idempotency
 *     key, in which case {@code order} is the order placed then and nothing new was created
 */
public record Placement(Order order, boolean replayed) {}
