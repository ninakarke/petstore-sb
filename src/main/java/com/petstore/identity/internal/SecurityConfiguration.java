package com.petstore.identity.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security of the whole application.
 *
 * <p>Replaces the legacy {@code SignOnFilter} and its {@code signon-config.xml}. At this stage
 * every page is public, as the catalog was in the legacy application, but the protections the
 * legacy application lacked are already on: every state-changing request must carry a CSRF token,
 * and responses carry the standard security headers.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

  /** Role of every user of the storefront. */
  static final String CUSTOMER = "CUSTOMER";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /**
   * Passwords are stored as salted hashes that name their own algorithm, so the algorithm can be
   * strengthened later without invalidating existing passwords. The legacy application stored and
   * compared passwords as plain text.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** Looks users up in the user store. Legacy: {@code SignOnEJB.authenticate}. */
  @Bean
  UserDetailsService userDetailsService(UserStore users) {
    return username ->
        users
            .find(username)
            .map(user ->
                User.withUsername(user.username())
                    .password(user.passwordHash())
                    .roles(CUSTOMER)
                    .build())
            .orElseThrow(() -> new UsernameNotFoundException(username));
  }
}
