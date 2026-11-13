package com.petstore.identity.internal.api;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.petstore.identity.internal.IdentityProperties;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Issues and verifies the bearer tokens of the JSON API.
 *
 * <p>A token is a JWT signed with HMAC-SHA256. It names the user and expires after a short time.
 * The legacy application had no programmatic interface and therefore nothing like this; it is new
 * code that exists only for the JSON API. The web pages do not use tokens.
 */
@Component
public class ApiTokens {

  static final String ISSUER = "petstore";
  private static final int MINIMUM_KEY_BYTES = 32;
  private static final Logger log = LoggerFactory.getLogger(ApiTokens.class);

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Duration lifetime;

  ApiTokens(IdentityProperties properties) {
    SecretKey key = key(properties.token().secret());
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    this.lifetime = properties.token().lifetime();
  }

  private static SecretKey key(String configuredSecret) {
    byte[] bytes;
    if (configuredSecret == null || configuredSecret.isBlank()) {
      bytes = new byte[MINIMUM_KEY_BYTES];
      new SecureRandom().nextBytes(bytes);
      log.info(
          "API tokens: no signing key configured, generated one for this run."
              + " Tokens stop being valid when the application restarts.");
    } else {
      bytes = Base64.getDecoder().decode(configuredSecret.trim());
      if (bytes.length < MINIMUM_KEY_BYTES) {
        throw new IllegalStateException(
            "petstore.security.token.secret must be at least " + MINIMUM_KEY_BYTES + " bytes");
      }
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  /** Issues a token for a user who has just proved who they are. */
  public String issue(String username) {
    return issue(username, Instant.now());
  }

  String issue(String username, Instant issuedAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(username)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(lifetime))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public Duration lifetime() {
    return lifetime;
  }

  /** Verifies signature and expiry of incoming tokens. */
  public JwtDecoder decoder() {
    return decoder;
  }
}
