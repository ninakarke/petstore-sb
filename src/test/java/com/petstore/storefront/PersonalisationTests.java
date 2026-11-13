package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.petstore.customer.CustomerDetails;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.CustomerService;
import com.petstore.customer.Profile;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * "My List" and the pet-tips banner, the two personalisation features of the storefront.
 *
 * <p>Legacy sources: {@code docroot/mylist.jsp}, {@code docroot/advice_banner.jsp}, {@code
 * BannerHelper}, and the {@code mylist} and {@code advicebanner} slots of {@code
 * screendefinitions_en_US.xml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PersonalisationTests {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private MockMvcTester mvc;
  @Autowired private CustomerService customers;

  // ---- My List ---------------------------------------------------------------------------------

  @Test
  @DisplayName(
      "legacy mylist.jsp: a customer with My List on sees the products of their favourite category")
  void myListShowsTheFavouriteCategory() {
    // Seed customer j2ee: favourite category REPTILES, My List on.
    for (String url : new String[] {"/", "/categories/FISH", "/items/EST-1", "/cart", "/account"}) {
      assertThat(mvc.get().uri(url).with(user("j2ee")))
          .as(url)
          .bodyText()
          .contains("class=\"mylist\"")
          .contains("My List")
          .contains("href=\"/products/RP-LI-02\">Iguana</a>")
          .contains("href=\"/products/RP-SN-01\">Rattlesnake</a>");
    }
  }

  @Test
  void visitorWhoIsNotSignedInSeesNoMyList() {
    assertThat(mvc.get().uri("/")).bodyText().doesNotContain("My List");
  }

  @Test
  @DisplayName("legacy: a customer with My List off sees none")
  void customerWithMyListOffSeesNone() {
    String username = customer(new Profile("en_US", "DOGS", false, false));

    assertThat(mvc.get().uri("/").with(user(username))).bodyText().doesNotContain("My List");
  }

  @Test
  void signedInUserWithoutACustomerSeesNoMyList() {
    assertThat(mvc.get().uri("/").with(user("nobody-yet")))
        .hasStatusOk()
        .bodyText()
        .doesNotContain("My List");
  }

  // ---- pet-tips banner -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "legacy advice_banner.jsp: with banners on, home and cart show the banner of the favourite category")
  void bannerIsShownOnHomeAndCart() {
    String username = customer(new Profile("en_US", "DOGS", false, true));

    for (String url : new String[] {"/", "/cart"}) {
      assertThat(mvc.get().uri(url).with(user(username)))
          .as(url)
          .bodyText()
          .contains("class=\"advice-banner\"")
          .contains("src=\"/images/banner_dogs.gif\"");
    }
  }

  @Test
  @DisplayName("legacy screen definitions: other pages have no banner slot")
  void bannerIsNotShownOnOtherPages() {
    String username = customer(new Profile("en_US", "DOGS", false, true));

    for (String url : new String[] {"/categories/FISH", "/items/EST-1", "/account"}) {
      assertThat(mvc.get().uri(url).with(user(username)))
          .as(url)
          .bodyText()
          .doesNotContain("advice-banner");
    }
  }

  @Test
  @DisplayName("legacy: a customer with banners off sees none")
  void customerWithBannersOffSeesNone() {
    // Seed customer j2ee has banners off.
    assertThat(mvc.get().uri("/").with(user("j2ee"))).bodyText().doesNotContain("advice-banner");
    assertThat(mvc.get().uri("/")).bodyText().doesNotContain("advice-banner");
  }

  @Test
  @DisplayName(
      "legacy BannerHelper: each category has its banner; anything else gets the dogs banner")
  void bannerPictureFollowsTheFavouriteCategory() {
    assertThat(bannerShownTo(customer(new Profile("en_US", "FISH", false, true))))
        .contains("banner_fish.gif");
    assertThat(bannerShownTo(customer(new Profile("en_US", "BIRDS", false, true))))
        .contains("banner_birds.gif");
    assertThat(bannerShownTo(customer(new Profile("en_US", "CATS", false, true))))
        .contains("banner_cats.gif");
    assertThat(bannerShownTo(customer(new Profile("en_US", "REPTILES", false, true))))
        .contains("banner_reptiles.gif");
    assertThat(bannerShownTo(customer(new Profile("en_US", "UNICORNS", false, true))))
        .contains("banner_dogs.gif");
  }

  private String bannerShownTo(String username) {
    try {
      return mvc.get().uri("/").with(user(username)).exchange().getResponse().getContentAsString();
    } catch (java.io.UnsupportedEncodingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** Creates a customer with the given preferences and returns the user name. */
  private String customer(Profile profile) {
    String username = "personal" + COUNTER.incrementAndGet();
    customers.create(
        username,
        new CustomerDetails(
            CustomerFixtures.contactInfo(), CustomerFixtures.creditCard(), profile));
    return username;
  }
}
