package com.petstore.customer;

/**
 * A customer's preferences.
 *
 * <p>Legacy equivalent: the {@code Profile} entity bean.
 *
 * @param preferredLanguage a locale code such as {@code en_US}. Kept because it is part of the
 *     legacy data; the store is English only and does not act on it.
 * @param favoriteCategory id of a catalog category, such as {@code DOGS}
 * @param myListPreference whether pages show the products of the favourite category
 * @param bannerPreference whether pages show the pet-tips banner
 */
public record Profile(
    String preferredLanguage,
    String favoriteCategory,
    boolean myListPreference,
    boolean bannerPreference) {

  /** Legacy: the {@code Default...} constants of {@code ProfileLocalHome}. */
  public static Profile defaults() {
    return new Profile("en_US", null, true, true);
  }
}
