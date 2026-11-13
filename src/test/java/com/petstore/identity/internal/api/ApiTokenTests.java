package com.petstore.identity.internal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.petstore.identity.internal.IdentityProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Bearer tokens of the JSON API.
 *
 * <p>There is no legacy counterpart: the legacy application had no programmatic interface. The
 * tokens are checked against the same users as the sign-in page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTokenTests {

  private static final String TOKEN = "/api/v1/auth/token";
  private static final String ME = "/api/v1/auth/me";

  @Autowired private MockMvcTester mvc;
  @Autowired private ApiTokens tokens;

  // ---- obtaining a token ----------------------------------------------------------------------

  @Test
  void userNameAndPasswordAreExchangedForAToken() {
    assertThat(requestToken("j2ee", "j2ee"))
        .hasStatusOk()
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .bodyJson()
        .convertTo(Map.class)
        .satisfies(body -> {
          assertThat(body.get("tokenType")).isEqualTo("Bearer");
          assertThat(body.get("expiresIn")).isEqualTo(900);
          assertThat((String) body.get("accessToken")).matches("[\\w-]+\\.[\\w-]+\\.[\\w-]+");
        });
  }

  @Test
  void wrongPasswordGetsNoToken() {
    assertThat(requestToken("j2ee", "wrong"))
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyText()
        .doesNotContain("accessToken");
  }

  @Test
  void unknownUserGetsTheSameAnswerAsAWrongPassword() {
    MvcTestResult unknown = requestToken("nobody", "j2ee");
    MvcTestResult wrong = requestToken("j2ee", "wrong");

    assertThat(unknown).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(unknown)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("The user name or password is not correct");
    assertThat(wrong)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("The user name or password is not correct");
  }

  @Test
  void requestWithoutUserNameOrPasswordIsABadRequest() {
    assertThat(mvc.post().uri(TOKEN).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName(
      "the API is stateless: asking for a token creates no session and needs no CSRF token")
  void tokenRequestIsStateless() {
    MvcTestResult result = requestToken("j2ee", "j2ee");

    assertThat(result).hasStatusOk();
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  // ---- using a token --------------------------------------------------------------------------

  @Test
  void protectedEndpointWithoutATokenIsUnauthorized() {
    MvcTestResult result = mvc.get().uri(ME).exchange();

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Bearer");
  }

  @Test
  void protectedEndpointWithATokenAnswersForThatUser() {
    assertThat(mvc.get().uri(ME).header("Authorization", "Bearer " + tokens.issue("j2ee")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.username")
        .isEqualTo("j2ee");
  }

  @Test
  void tokenFromTheTokenEndpointIsAccepted() {
    Map<?, ?> body =
        (Map<?, ?>)
            org.assertj.core.api.Assertions.assertThat(requestToken("shopper", "j2ee"))
                .bodyJson()
                .convertTo(Map.class)
                .actual();

    assertThat(mvc.get().uri(ME).header("Authorization", "Bearer " + body.get("accessToken")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.username")
        .isEqualTo("shopper");
  }

  @Test
  void tamperedTokenIsRejected() {
    String token = tokens.issue("j2ee");
    String tampered = token.substring(0, token.length() - 4) + "AAAA";

    assertThat(mvc.get().uri(ME).header("Authorization", "Bearer " + tampered))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    String otherSecret = Base64.getEncoder().encodeToString(new byte[32]);
    ApiTokens foreign = new ApiTokens(properties(otherSecret, Duration.ofMinutes(15)));

    assertThat(mvc.get().uri(ME).header("Authorization", "Bearer " + foreign.issue("j2ee")))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void expiredTokenIsRejected() {
    String expired = tokens.issue("j2ee", Instant.now().minus(Duration.ofHours(2)));

    assertThat(mvc.get().uri(ME).header("Authorization", "Bearer " + expired))
        .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  // ---- separation from the web pages ----------------------------------------------------------

  @Test
  @DisplayName("a browser's sign-in session does not authenticate an API call")
  void signInSessionDoesNotAuthenticateTheApi() {
    MockHttpSession session = new MockHttpSession();
    mvc.post()
        .uri("/signin")
        .param("j_username", "j2ee")
        .param("j_password", "j2ee")
        .session(session)
        .with(csrf())
        .exchange();

    assertThat(mvc.get().uri(ME).session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void catalogApiStaysPublic() {
    assertThat(mvc.get().uri("/api/v1/catalog/categories")).hasStatusOk();
  }

  @Test
  void pagesKeepTheirOwnRules() {
    MvcTestResult result = mvc.get().uri("/account").exchange();

    assertThat(result).hasStatus(HttpStatus.FOUND);
    assertThat(result.getResponse().getRedirectedUrl()).endsWith("/signin");
  }

  @Test
  void apiDescriptionOffersBearerAuthentication() {
    assertThat(mvc.get().uri("/v3/api-docs"))
        .bodyText()
        .contains("bearer-jwt")
        .contains("/api/v1/auth/token");
  }

  // ---- signing key ----------------------------------------------------------------------------

  @Test
  void configuredSigningKeyKeepsTokensValidAcrossInstances() {
    String secret =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    ApiTokens first = new ApiTokens(properties(secret, Duration.ofMinutes(5)));
    ApiTokens second = new ApiTokens(properties(secret, Duration.ofMinutes(5)));

    assertThat(second.decoder().decode(first.issue("j2ee")).getSubject()).isEqualTo("j2ee");
    assertThat(first.lifetime()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void signingKeyThatIsTooShortIsRefusedAtStartup() {
    String tooShort = Base64.getEncoder().encodeToString("short".getBytes());

    assertThatThrownBy(() -> new ApiTokens(properties(tooShort, Duration.ofMinutes(5))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 bytes");
  }

  private static IdentityProperties properties(String secret, Duration lifetime) {
    return new IdentityProperties(
        List.of(), List.of(), new IdentityProperties.Token(secret, lifetime));
  }

  private MvcTestResult requestToken(String username, String password) {
    return mvc.post()
        .uri(TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
        .exchange();
  }
}
