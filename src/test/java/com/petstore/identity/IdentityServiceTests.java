package com.petstore.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petstore.identity.internal.InMemoryUserStore;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Rules for creating a user.
 *
 * <p>Legacy sources: {@code components/signon/.../user/ejb/UserEJB.java} ({@code ejbCreate}, {@code
 * matchPassword}), {@code UserLocal.java} (the length limits) and {@code
 * apps/petstore/.../web/actions/CreateUserHTMLAction.java}.
 */
class IdentityServiceTests {

  private final InMemoryUserStore store = new InMemoryUserStore();
  private final PasswordEncoder encoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();
  private final IdentityService identity = new IdentityService(store, encoder);

  @Test
  void registeredUserExists() {
    identity.register("duke", "secret", "secret");

    assertThat(identity.exists("duke")).isTrue();
    assertThat(identity.exists("nobody")).isFalse();
    assertThat(identity.exists(null)).isFalse();
  }

  @Test
  @DisplayName("fixed: the password is stored as a salted hash (legacy stored it as typed)")
  void passwordIsStoredHashed() {
    identity.register("duke", "secret", "secret");
    identity.register("tux", "secret", "secret");

    String dukesHash = store.find("duke").orElseThrow().passwordHash();
    String tuxsHash = store.find("tux").orElseThrow().passwordHash();
    assertThat(dukesHash).doesNotContain("secret").startsWith("{bcrypt}");
    assertThat(encoder.matches("secret", dukesHash)).isTrue();
    assertThat(encoder.matches("Secret", dukesHash)).isFalse();
    // Salted: the same password gives a different hash for each user.
    assertThat(dukesHash).isNotEqualTo(tuxsHash);
  }

  @Test
  @DisplayName("legacy: user name and password may be up to 25 characters")
  void maximumLengthsAreAccepted() {
    assertThatCode(() -> identity.register("u".repeat(25), "p".repeat(25), "p".repeat(25)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("legacy: a user name longer than 25 characters is refused")
  void longUserNameIsRefused() {
    assertRefused(
        () -> identity.register("u".repeat(26), "secret", "secret"),
        RegistrationProblem.USERNAME_TOO_LONG);
  }

  @Test
  @DisplayName("legacy: a password longer than 25 characters is refused")
  void longPasswordIsRefused() {
    assertRefused(
        () -> identity.register("duke", "p".repeat(26), "p".repeat(26)),
        RegistrationProblem.PASSWORD_TOO_LONG);
  }

  @Test
  @DisplayName("legacy: a user name may not contain % or *")
  void forbiddenCharactersInUserNameAreRefused() {
    assertRefused(
        () -> identity.register("du%ke", "secret", "secret"),
        RegistrationProblem.USERNAME_HAS_FORBIDDEN_CHARACTER);
    assertRefused(
        () -> identity.register("duke*", "secret", "secret"),
        RegistrationProblem.USERNAME_HAS_FORBIDDEN_CHARACTER);
  }

  @Test
  @DisplayName("legacy: a user name that is taken is refused")
  void takenUserNameIsRefused() {
    identity.register("duke", "secret", "secret");

    assertRefused(
        () -> identity.register("duke", "other", "other"), RegistrationProblem.USERNAME_TAKEN);
    assertThat(encoder.matches("secret", store.find("duke").orElseThrow().passwordHash())).isTrue();
  }

  @Test
  @DisplayName("fixed: the repeated password must match (legacy read it and never compared it)")
  void differingPasswordsAreRefused() {
    assertRefused(
        () -> identity.register("duke", "secret", "secrte"), RegistrationProblem.PASSWORDS_DIFFER);
    assertRefused(
        () -> identity.register("duke", "secret", null), RegistrationProblem.PASSWORDS_DIFFER);
    assertThat(store.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("fixed: a blank user name or password is refused (legacy accepted empty values)")
  void blankValuesAreRefused() {
    assertRefused(
        () -> identity.register("", "secret", "secret"), RegistrationProblem.USERNAME_REQUIRED);
    assertRefused(
        () -> identity.register(null, "secret", "secret"), RegistrationProblem.USERNAME_REQUIRED);
    assertRefused(() -> identity.register("duke", " ", " "), RegistrationProblem.PASSWORD_REQUIRED);
    assertRefused(
        () -> identity.register("duke", null, null), RegistrationProblem.PASSWORD_REQUIRED);
  }

  private static void assertRefused(ThrowingCallable registration, RegistrationProblem problem) {
    assertThatThrownBy(registration)
        .isInstanceOfSatisfying(
            RegistrationException.class,
            exception -> assertThat(exception.problem()).isEqualTo(problem));
  }
}
