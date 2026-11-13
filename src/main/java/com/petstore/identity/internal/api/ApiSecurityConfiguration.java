package com.petstore.identity.internal.api;

import com.petstore.identity.internal.IdentityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security of the JSON API, kept apart from the security of the web pages.
 *
 * <p>Everything under {@code /api} is stateless: no session is created or read, so a browser's
 * sign-in session does not authenticate an API call and there is nothing for a cross-site request
 * to ride on. The catalog can be read by anyone, as on the pages. Every other endpoint needs a
 * bearer token obtained from the token endpoint.
 */
@Configuration(proxyBeanMethods = false)
class ApiSecurityConfiguration {

  static final String TOKEN_PATH = "/api/v1/auth/token";

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain apiSecurity(
      HttpSecurity http, IdentityProperties properties, ApiTokens tokens) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests ->
            requests
                .requestMatchers(HttpMethod.GET, properties.publicApiPaths().toArray(String[]::new))
                .permitAll()
                .requestMatchers(HttpMethod.POST, TOKEN_PATH)
                .permitAll()
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(
            resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(tokens.decoder())));
    return http.build();
  }
}
