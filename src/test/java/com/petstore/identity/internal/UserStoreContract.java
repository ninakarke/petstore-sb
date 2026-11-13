package com.petstore.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.identity.internal.UserStore.StoredUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The contract every {@link UserStore} adapter must fulfil. Written once against the port and run
 * for each data store by a small subclass.
 *
 * <p>Legacy source: the {@code User} entity bean, {@code components/signon/.../user/ejb/}.
 */
public abstract class UserStoreContract {

  /** The adapter under test. */
  protected abstract UserStore store();

  /** Hook for stores that do not roll back after a test. */
  protected void emptyTheStore() {}

  @BeforeEach
  void startEmpty() {
    emptyTheStore();
  }

  @Test
  void newStoreIsEmpty() {
    assertThat(store().isEmpty()).isTrue();
    assertThat(store().find("j2ee")).isEmpty();
  }

  @Test
  void createdUserCanBeFoundWithItsPasswordHash() {
    assertThat(store().create("j2ee", "hash-1")).isTrue();

    assertThat(store().find("j2ee")).contains(new StoredUser("j2ee", "hash-1"));
    assertThat(store().isEmpty()).isFalse();
  }

  @Test
  @DisplayName("legacy: a user name can be taken only once")
  void secondUserOfTheSameNameIsRefusedAndTheFirstIsKept() {
    store().create("j2ee", "hash-1");

    assertThat(store().create("j2ee", "hash-2")).isFalse();

    assertThat(store().find("j2ee")).contains(new StoredUser("j2ee", "hash-1"));
  }

  @Test
  @DisplayName("legacy: user names are compared exactly, including case")
  void userNamesAreCaseSensitive() {
    store().create("j2ee", "hash-1");

    assertThat(store().find("J2EE")).isEmpty();
    assertThat(store().create("J2EE", "hash-2")).isTrue();
  }

  @Test
  void aLongHashFits() {
    String hash = "{bcrypt}" + "x".repeat(60);

    store().create("shopper", hash);

    assertThat(store().find("shopper")).map(StoredUser::passwordHash).contains(hash);
  }

  @Test
  @DisplayName("legacy: a user name may be 25 characters long")
  void userNameOfMaximumLengthFits() {
    String name = "u".repeat(25);

    assertThat(store().create(name, "hash")).isTrue();
    assertThat(store().find(name)).isPresent();
  }
}
