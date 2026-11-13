package com.petstore.storefront.internal.web;

import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Category;
import com.petstore.catalog.Product;
import com.petstore.customer.CustomerService;
import com.petstore.customer.Profile;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies what the page layout needs on every page.
 *
 * <p>In the legacy application each layout fragment fetched its own data: {@code sidebar.jsp} and
 * {@code mylist.jsp} created a catalog helper and queried the database from inside the page, and
 * {@code advice_banner.jsp} read the customer entity bean kept in the session. Here the data is put
 * in the model before the page is rendered, and templates only display it.
 */
@ControllerAdvice
class StorefrontModelAdvice {

  /** Legacy: {@code mylist.jsp} asked for the first ten products of the favourite category. */
  static final int MY_LIST_SIZE = 10;

  /** Legacy: the categories {@code BannerHelper} had a banner for. */
  private static final Set<String> BANNER_CATEGORIES =
      Set.of("dogs", "cats", "reptiles", "birds", "fish");

  private final CatalogService catalog;
  private final CustomerService customers;

  StorefrontModelAdvice(CatalogService catalog, CustomerService customers) {
    this.catalog = catalog;
    this.customers = customers;
  }

  /** The category list of the sidebar. Legacy: {@code sidebar.jsp}. */
  @ModelAttribute("sidebarCategories")
  List<Category> sidebarCategories() {
    return catalog.categories();
  }

  /**
   * "My List": the products of the signed-in customer's favourite category, if they switched the
   * feature on. Empty for everyone else. Legacy: {@code mylist.jsp}.
   */
  @ModelAttribute("myList")
  List<Product> myList(Principal user) {
    return profile(user)
        .filter(Profile::myListPreference)
        .map(Profile::favoriteCategory)
        .filter(category -> category != null && !category.isBlank())
        .map(category -> catalog.products(category, 0, MY_LIST_SIZE).items())
        .orElse(List.of());
  }

  /**
   * The picture of the pet-tips banner for the signed-in customer, if they switched the feature on;
   * {@code null} for everyone else. Pages decide whether they show it. Legacy: {@code
   * advice_banner.jsp}.
   */
  @ModelAttribute("adviceBanner")
  String adviceBanner(Principal user) {
    return profile(user)
        .filter(Profile::bannerPreference)
        .map(profile -> bannerFor(profile.favoriteCategory()))
        .orElse(null);
  }

  /**
   * The banner picture for a category. A category without a banner of its own gets the dogs banner.
   * Same rule as the legacy {@code BannerHelper}.
   */
  static String bannerFor(String categoryId) {
    String category = categoryId == null ? "" : categoryId.toLowerCase(Locale.ROOT);
    return "banner_" + (BANNER_CATEGORIES.contains(category) ? category : "dogs") + ".gif";
  }

  private Optional<Profile> profile(Principal user) {
    if (user == null) {
      return Optional.empty();
    }
    return customers.find(user.getName()).map(customer -> customer.profile());
  }
}
