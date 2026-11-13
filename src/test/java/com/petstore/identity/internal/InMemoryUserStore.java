package com.petstore.identity.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A user store that keeps everything in memory, for fast tests of the logic above the port. Held to
 * the same {@link UserStoreContract} as the real adapters.
 */
public class InMemoryUserStore implements UserStore {

  private final Map<String, String> hashes = new LinkedHashMap<>();

  @Override
  public Optional<StoredUser> find(String username) {
    return Optional.ofNullable(hashes.get(username)).map(hash -> new StoredUser(username, hash));
  }

  @Override
  public boolean create(String username, String passwordHash) {
    return hashes.putIfAbsent(username, passwordHash) == null;
  }

  @Override
  public boolean isEmpty() {
    return hashes.isEmpty();
  }

  public int size() {
    return hashes.size();
  }

  public void clear() {
    hashes.clear();
  }
}
