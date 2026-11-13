package com.petstore.catalog;

/**
 * A product in one language, for example {@code FI-SW-01} / "Angelfish". A product groups the items
 * that can actually be bought.
 *
 * <p>Legacy equivalent: {@code com.sun.j2ee.blueprints.catalog.model.Product}.
 */
public record Product(String id, String name, String description) {}
