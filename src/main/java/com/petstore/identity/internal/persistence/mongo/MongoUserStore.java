package com.petstore.identity.internal.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.petstore.identity.internal.UserStore;
import com.petstore.shared.DocumentStore;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
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
  public List<StoredUser> usersFrom(String startUsername, int limit) {
    Query query = startUsername == null ? new Query() : query(where("_id").gte(startUsername));
    return mongo.find(query.with(Sort.by("_id")).limit(limit), UserDocument.class).stream()
        .map(user -> new StoredUser(user.username(), user.passwordHash()))
        .toList();
  }

  @Override
  public boolean isEmpty() {
    return !mongo.exists(new Query(), UserDocument.class);
  }
}
