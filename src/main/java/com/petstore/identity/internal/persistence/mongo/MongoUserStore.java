package com.petstore.identity.internal.persistence.mongo;

import com.petstore.identity.internal.UserStore;
import com.petstore.shared.DocumentStore;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/** MongoDB adapter of the identity port. Replaces the container-managed {@code UserEJB}. */
@Repository
@DocumentStore
class MongoUserStore implements UserStore {

  private final MongoOperations mongo;

  MongoUserStore(MongoOperations mongo) {
    this.mongo = mongo;
  }

  @Override
  public Optional<StoredUser> find(String username) {
    return Optional.ofNullable(mongo.findById(username, UserDocument.class))
        .map(user -> new StoredUser(user.username(), user.passwordHash()));
  }

  /**
   * One insert, no check first: the server refuses a second document with the same key, also when
   * two requests create the same name at the same moment.
   */
  @Override
  public boolean create(String username, String passwordHash) {
    try {
      mongo.insert(new UserDocument(username, passwordHash));
      return true;
    } catch (DuplicateKeyException alreadyExists) {
      return false;
    }
  }

  @Override
  public boolean isEmpty() {
    return !mongo.exists(new Query(), UserDocument.class);
  }
}
