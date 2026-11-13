package com.petstore.customer.internal;

import com.petstore.customer.Account;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;
import com.petstore.shared.SeedDocument;
import com.petstore.shared.SeedProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Loads the demo customers of the seed document into an empty customer store, once, at startup.
 *
 * <p>Replaces the legacy {@code CustomerPopulator} and the populators it chained ({@code
 * AccountPopulator}, {@code ContactInfoPopulator}, {@code AddressPopulator}, {@code
 * CreditCardPopulator}, {@code ProfilePopulator}). The legacy populator created each customer with
 * default details first and then re-pointed its relationships, leaving the default rows behind.
 * Here each customer is written once, complete.
 */
@Component
@Order(30)
class CustomerSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CustomerSeeder.class);

  private final CustomerStore customers;
  private final SeedProperties properties;

  CustomerSeeder(CustomerStore customers, SeedProperties properties) {
    this.customers = customers;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.enabled() || !customers.isEmpty()) {
      return;
    }
    List<Element> seedCustomers =
        SeedDocument.load(properties.location()).elements("Customers", "Customer");
    for (Element customer : seedCustomers) {
      Element account = SeedDocument.child(customer, "Account").orElseThrow();
      Element contact = SeedDocument.child(account, "ContactInfo").orElseThrow();
      Element address = SeedDocument.child(contact, "Address").orElseThrow();
      Element card = SeedDocument.child(account, "CreditCard").orElseThrow();
      Element profile = SeedDocument.child(customer, "Profile").orElseThrow();
      List<String> streets = SeedDocument.texts(address, "StreetName");

      customers.save(
          new Customer(
              customer.getAttribute("id"),
              new Account(
                  Account.ACTIVE,
                  new ContactInfo(
                      text(contact, "GivenName"),
                      text(contact, "FamilyName"),
                      new Address(
                          streets.isEmpty() ? null : streets.get(0),
                          streets.size() > 1 ? streets.get(1) : null,
                          text(address, "City"),
                          text(address, "State"),
                          text(address, "ZipCode"),
                          text(address, "Country")),
                      text(contact, "Email"),
                      text(contact, "Phone")),
                  new CreditCard(
                      text(card, "CardNumber"), text(card, "CardType"), text(card, "ExpiryDate"))),
              new Profile(
                  text(profile, "PreferredLanguage"),
                  text(profile, "FavoriteCategory"),
                  Boolean.parseBoolean(text(profile, "MyListPreference")),
                  Boolean.parseBoolean(text(profile, "BannerPreference")))));
    }
    log.info("Customer seed data: loaded {} customers", seedCustomers.size());
  }

  private static String text(Element parent, String name) {
    return SeedDocument.text(parent, name).orElse(null);
  }
}
