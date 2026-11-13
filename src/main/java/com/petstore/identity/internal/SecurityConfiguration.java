package com.petstore.identity.internal;

import com.petstore.identity.internal.web.KeepLanguageOnSignOut;
import com.petstore.identity.internal.web.RememberUsernameFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security of the web pages.
 *
 * <p>Replaces the legacy {@code SignOnFilter} and its {@code signon-config.xml}:
 *
 * <ul>
 *   <li>the list of protected pages comes from configuration, as it did there; everything else,
 *       such as the catalog and the cart, is public;
 *   <li>an anonymous request for a protected page is sent to the sign-in page and, after signing
 *       in, back to the page that was asked for (legacy: {@code j_signon_original_url});
 *   <li>the form fields keep their legacy names, {@code j_username} and {@code j_password}.
 * </ul>
 *
 * <p>What the legacy filter did not have: CSRF protection on every state-changing request, standard
 * security headers, a new session id at sign-in, and sign-out by POST only.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

  /** Role of every user of the storefront. */
  static final String CUSTOMER = "CUSTOMER";

  static final String SIGN_IN_PATH = "/signin";
  static final String SIGN_OUT_PATH = "/signout";
  static final String SIGNED_OUT_PATH = "/signedout";

  /** Legacy: {@code SignOnFilter.FORM_USER_NAME} and {@code FORM_PASSWORD}. */
  static final String USERNAME_PARAMETER = "j_username";

  static final String PASSWORD_PARAMETER = "j_password";

  @Bean
  SecurityFilterChain webSecurity(HttpSecurity http, IdentityProperties properties)
      throws Exception {
    KeepLanguageOnSignOut keepLanguage = new KeepLanguageOnSignOut(SIGNED_OUT_PATH);
    http.authorizeHttpRequests(requests ->
            requests
                .requestMatchers(properties.protectedPaths().toArray(String[]::new))
                .authenticated()
                .anyRequest()
                .permitAll())
        .formLogin(login ->
            login
                .loginPage(SIGN_IN_PATH)
                .loginProcessingUrl(SIGN_IN_PATH)
                .usernameParameter(USERNAME_PARAMETER)
                .passwordParameter(PASSWORD_PARAMETER)
                .failureUrl(SIGN_IN_PATH + "?error")
                .defaultSuccessUrl("/"))
        .logout(logout ->
            logout
                .logoutUrl(SIGN_OUT_PATH)
                .addLogoutHandler(keepLanguage)
                .logoutSuccessHandler(keepLanguage))
        .addFilterBefore(
            new RememberUsernameFilter(SIGN_IN_PATH, USERNAME_PARAMETER),
            UsernamePasswordAuthenticationFilter.class);
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
