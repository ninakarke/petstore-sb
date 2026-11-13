package com.petstore.identity.internal;

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

  /** A user as stored: the name and the hash of the password, never the password. */
  record StoredUser(String username, String passwordHash) {}
}
