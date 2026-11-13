package com.petstore.identity.internal;

import java.util.List;
import java.util.Optional;

/**
 * The persistence port of the identity module.
 *
 * <p>Replaces the home and local interfaces of the legacy {@code User} entity bean ({@code
 * findByPrimaryKey}, {@code create}). Every adapter must pass the same contract test suite.
 */
public interface UserStore {

  Optional<StoredUser> find(String username);

  /**
   * Stores a new user.
   *
   * @return {@code false}, and nothing is stored, if a user of that name already exists
   */
  boolean create(String username, String passwordHash);

  boolean isEmpty();

  /**
   * Users in the store's key order: at most {@code limit} of them, starting with the user of the
   * given name, or with the first one if the name is {@code null}. If there is no user of that
   * name, the list starts with the next one in key order. Used to copy users to another store.
   */
  List<StoredUser> usersFrom(String startUsername, int limit);

  /** A user as stored: the name and the hash of the password, never the password. */
  record StoredUser(String username, String passwordHash) {}
}
