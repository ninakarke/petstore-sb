package com.petstore.identity.internal.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.identity.internal.UserStore;
import com.petstore.identity.internal.UserStoreContract;
import com.petstore.testsupport.MongoStoreTest;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** The user store contract on a real MongoDB server. */
@MongoStoreTest
@Import(MongoUserStore.class)
class MongoUserStoreTests extends UserStoreContract {

  @Autowired private UserStore store;
  @Autowired private MongoTemplate mongo;

  @Override
  protected UserStore store() {
    return store;
  }

  @Override
  protected void emptyTheStore() {
    mongo.remove(new Query(), "users");
  }

  @Test
  @DisplayName("a user is one small document keyed by the user name")
  void userIsKeyedByName() {
    store.create("duke", "{noop}secret");

    Document user = mongo.getCollection("users").find(new Document("_id", "duke")).first();

    assertThat(user).containsOnlyKeys("_id", "passwordHash");
  }
}
