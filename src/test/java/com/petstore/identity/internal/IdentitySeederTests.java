package com.petstore.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.shared.SeedProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Loading the demo users from the legacy seed file.
 *
 * <p>Legacy sources: {@code apps/petstore/.../tools/populate/UserPopulator.java} and {@code
 * docroot/populate/Populate-UTF8.xml}.
 */
class IdentitySeederTests {

  private static final ClassPathResource SEED = new ClassPathResource("seed/Populate-UTF8.xml");

  private final InMemoryUserStore store = new InMemoryUserStore();
  private final PasswordEncoder encoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();

  @Test
  @DisplayName("legacy seed data: four demo users, all with the password j2ee")
  void loadsTheFourDemoUsers() {
    seed(true);

    assertThat(store.size()).isEqualTo(4);
    for (String username : new String[] {"j2ee", "j2ee-ja", "shopper", "j2ee-zh"}) {
      String hash = store.find(username).orElseThrow().passwordHash();
      assertThat(encoder.matches("j2ee", hash)).as(username).isTrue();
    }
  }

  @Test
  @DisplayName("fixed: seed passwords are hashed before they are stored")
  void seedPasswordsAreNotStoredAsTyped() {
    seed(true);

    assertThat(store.find("j2ee").orElseThrow().passwordHash())
        .isNotEqualTo("j2ee")
        .startsWith("{bcrypt}");
  }

  @Test
  void doesNothingWhenUsersAlreadyExist() {
    store.create("only", "hash");

    seed(true);

    assertThat(store.size()).isEqualTo(1);
  }

  @Test
  void doesNothingWhenSeedingIsSwitchedOff() {
    seed(false);

    assertThat(store.isEmpty()).isTrue();
  }

  private void seed(boolean enabled) {
    new IdentitySeeder(store, encoder, new SeedProperties(enabled, SEED)).run(null);
  }
}
