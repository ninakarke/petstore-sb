package com.petstore.identity.internal;

import com.petstore.shared.SeedDocument;
import com.petstore.shared.SeedProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

/**
 * Loads the demo users of the seed document into an empty user store, once, at startup.
 *
 * <p>Replaces the legacy {@code UserPopulator}. The seed file lists each user's password in the
 * clear; it is hashed before it is stored, so no password is ever written to the database as typed.
 */
@Component
@Order(20)
class IdentitySeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(IdentitySeeder.class);

  private final UserStore users;
  private final PasswordEncoder passwordEncoder;
  private final SeedProperties properties;

  IdentitySeeder(UserStore users, PasswordEncoder passwordEncoder, SeedProperties properties) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.enabled() || !users.isEmpty()) {
      return;
    }
    List<Element> seedUsers = SeedDocument.load(properties.location()).elements("Users", "User");
    for (Element user : seedUsers) {
      users.create(
          user.getAttribute("id"),
          passwordEncoder.encode(SeedDocument.text(user, "Password").orElseThrow()));
    }
    log.info("User seed data: loaded {} users", seedUsers.size());
  }
}
