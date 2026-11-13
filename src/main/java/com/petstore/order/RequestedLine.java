package com.petstore.order;

/**
 * A line of an order as a client asks for it: which item and how many. The price is deliberately
 * not part of it; it is always taken from the catalog.
 */
public record RequestedLine(String itemId, int quantity) {}
