package com.petstore.identity.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security baseline for the whole application.
 *
 * <p>Replaces the legacy {@code SignOnFilter} and its {@code signon-config.xml}. At this stage
 * every page is public, as the catalog was in the legacy application, but the protections the
 * legacy application lacked are already on: every state-changing request must carry a CSRF token,
 * and responses carry the standard security headers. Sign-in and the list of protected resources
 * are added with the identity slice.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /** No users yet. Declared so that the framework does not generate a default account. */
  @Bean
  UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }
}
