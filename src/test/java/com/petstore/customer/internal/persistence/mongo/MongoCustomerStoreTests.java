package com.petstore.customer.internal.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.internal.CustomerStore;
import com.petstore.customer.internal.CustomerStoreContract;
import com.petstore.testsupport.MongoStoreTest;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** The customer store contract on a real MongoDB server. */
@MongoStoreTest
@Import(MongoCustomerStore.class)
class MongoCustomerStoreTests extends CustomerStoreContract {

  @Autowired private CustomerStore store;
  @Autowired private MongoTemplate mongo;

  @Override
  protected CustomerStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    mongo.remove(new Query(), "customers");
  }

  @Test
  @DisplayName("a customer is one document: account, contact, address, card and profile inside it")
  void customerIsOneDocument() {
    store.save(CustomerFixtures.customer("duke"));

    assertThat(mongo.getCollection("customers").countDocuments()).isEqualTo(1);
    Document customer = mongo.getCollection("customers").find(new Document("_id", "duke")).first();
    assertThat(customer).containsOnlyKeys("_id", "account", "profile");
    Document account = customer.get("account", Document.class);
    assertThat(account).containsOnlyKeys("status", "contact", "creditCard");
    assertThat(account.get("contact", Document.class)).containsKey("address");
    assertThat(mongo.getCollectionNames()).doesNotContain("accounts", "addresses", "profiles");
  }

  @Test
  @DisplayName("saving a customer again replaces the document, it does not add one")
  void savingAgainReplacesTheDocument() {
    store.save(CustomerFixtures.customer("duke"));
    store.save(CustomerFixtures.customer("duke"));

    assertThat(mongo.getCollection("customers").countDocuments()).isEqualTo(1);
  }
}
