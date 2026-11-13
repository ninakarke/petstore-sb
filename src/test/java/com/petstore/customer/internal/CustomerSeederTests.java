package com.petstore.customer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.customer.Account;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.Profile;
import com.petstore.shared.SeedProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Loading the demo customers from the legacy seed file.
 *
 * <p>Legacy sources: {@code apps/petstore/.../tools/populate/CustomerPopulator.java} and the
 * populators it chains, and {@code docroot/populate/Populate-UTF8.xml}.
 */
class CustomerSeederTests {

  private static final ClassPathResource SEED = new ClassPathResource("seed/Populate-UTF8.xml");

  private final InMemoryCustomerStore store = new InMemoryCustomerStore();

  @Test
  @DisplayName("legacy seed data: four customers, one per demo user")
  void loadsTheFourDemoCustomers() {
    seed(true);

    assertThat(store.size()).isEqualTo(4);
    for (String userId : new String[] {"j2ee", "j2ee-ja", "shopper", "j2ee-zh"}) {
      assertThat(store.find(userId)).as(userId).isPresent();
    }
  }

  @Test
  @DisplayName("legacy seed data: customer j2ee, field by field")
  void mapsEveryFieldOfACustomer() {
    seed(true);

    assertThat(store.find("j2ee"))
        .contains(
            new Customer(
                "j2ee",
                new Account(
                    "active",
                    new ContactInfo(
                        "XYZ",
                        "ABC",
                        new Address(
                            "1234 Anywhere Street", "Unit 555", "Palo Alto", "CA", "94303", "USA"),
                        "aaa@bbb.ccc",
                        "555-555-5555"),
                    new CreditCard("123456789", "Meow Card", "04/04")),
                new Profile("en_US", "REPTILES", true, false)));
  }

  @Test
  @DisplayName("legacy seed data: the Japanese and Chinese demo customers prefer their language")
  void keepsThePreferredLanguages() {
    seed(true);

    assertThat(store.find("j2ee-ja").orElseThrow().profile().preferredLanguage())
        .isEqualTo("ja_JP");
    assertThat(store.find("j2ee-zh").orElseThrow().profile().preferredLanguage())
        .isEqualTo("zh_CN");
  }

  @Test
  void doesNothingWhenCustomersAlreadyExist() {
    store.save(CustomerFixtures.customer("only"));

    seed(true);

    assertThat(store.size()).isEqualTo(1);
  }

  @Test
  void doesNothingWhenSeedingIsSwitchedOff() {
    seed(false);

    assertThat(store.isEmpty()).isTrue();
  }

  private void seed(boolean enabled) {
    new CustomerSeeder(store, new SeedProperties(enabled, SEED)).run(null);
  }
}
