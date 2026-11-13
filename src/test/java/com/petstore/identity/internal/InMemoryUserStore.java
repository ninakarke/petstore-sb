package com.petstore.identity.internal;

import java.util.LinkedHashMap;
import java.util.List;
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
  public List<StoredUser> usersFrom(String startUsername, int limit) {
    return hashes.entrySet().stream()
        .filter(entry -> startUsername == null || entry.getKey().compareTo(startUsername) >= 0)
        .sorted(Map.Entry.comparingByKey())
        .limit(limit)
        .map(entry -> new StoredUser(entry.getKey(), entry.getValue()))
        .toList();
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
