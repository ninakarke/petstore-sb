package com.petstore.identity.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Signing in to the JSON API.
 *
 * <p>A client sends a user name and password once and receives a bearer token, which it then sends
 * with each call. The user name and password are checked against the same users, with the same
 * password hashing, as the sign-in page.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Obtain a bearer token for the protected endpoints.")
class TokenController {

  private final AuthenticationManager authenticationManager;
  private final ApiTokens tokens;

  TokenController(UserDetailsService users, PasswordEncoder passwordEncoder, ApiTokens tokens) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(passwordEncoder);
    this.authenticationManager = new ProviderManager(provider);
    this.tokens = tokens;
  }

  @PostMapping("/token")
  @Operation(
      summary = "Exchange a user name and password for a bearer token",
      description = "Send the returned token as 'Authorization: Bearer <token>'.")
  TokenResponse token(@RequestBody TokenRequest request) {
    if (request.username() == null || request.password() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "username and password are required");
    }
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.username(), request.password()));
      return new TokenResponse(
          "Bearer", tokens.issue(authentication.getName()), tokens.lifetime().toSeconds());
    } catch (AuthenticationException rejected) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "The user name or password is not correct", rejected);
    }
  }

  @GetMapping("/me")
  @Operation(summary = "Show who the presented token belongs to")
  @SecurityRequirement(name = "bearer-jwt")
  WhoAmI me(Authentication authentication) {
    return new WhoAmI(authentication.getName());
  }

  record TokenRequest(String username, String password) {}

  /**
   * @param tokenType always {@code Bearer}
   * @param accessToken the token to send with each call
   * @param expiresIn seconds until the token expires
   */
  record TokenResponse(String tokenType, String accessToken, long expiresIn) {}

  record WhoAmI(String username) {}
}
