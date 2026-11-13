package com.petstore.catalog;

/**
 * A catalog category in one language, for example {@code FISH} / "Fish".
 *
 * <p>Legacy equivalent: {@code com.sun.j2ee.blueprints.catalog.model.Category}.
 */
public record Category(String id, String name, String description) {}
