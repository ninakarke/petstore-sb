package com.petstore.identity.internal;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security settings.
 *
 * @param protectedPaths the pages that require a signed-in customer. Legacy equivalent: the {@code
 *     <constraint>} entries of {@code WEB-INF/signon-config.xml}.
 * @param publicApiPaths the JSON endpoints that may be read without a token
 * @param token settings of the bearer tokens of the JSON API
 */
@ConfigurationProperties("petstore.security")
public record IdentityProperties(
    @DefaultValue({"/account/**", "/checkout/**", "/orders/**"}) List<String> protectedPaths,
    @DefaultValue("/api/v1/catalog/**") List<String> publicApiPaths,
    @DefaultValue Token token) {

  /**
   * @param secret Base64-encoded key, at least 32 bytes, used to sign and verify tokens. If empty,
   *     a random key is generated at startup; tokens then stop being valid when the application
   *     restarts and are not shared between instances.
   * @param lifetime how long a token is valid
   */
  public record Token(@DefaultValue("") String secret, @DefaultValue("15m") Duration lifetime) {}
}
