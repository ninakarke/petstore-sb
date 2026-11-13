package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.identity.IdentityService;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Character encoding of requests and pages, checked on a real server with a real HTTP client,
 * because this is decided by the server and not by the application's own code.
 *
 * <p>Legacy source: {@code EncodingFilter}, which set the request encoding to UTF-8 before any
 * parameter was read. Browsers send form fields as bytes without naming the encoding; a server that
 * guesses wrong stores a name such as "Renée" as "RenÃ©e".
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RequestEncodingTests {

  private static final Pattern CSRF_FIELD = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

  @LocalServerPort private int port;
  @Autowired private IdentityService identity;

  @Test
  @DisplayName("legacy: form fields are read as UTF-8 although the request names no encoding")
  void formFieldsAreReadAsUtf8() throws IOException, InterruptedException {
    HttpClient browser = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    String username = "renée-zoë-" + System.nanoTime() % 100_000;

    HttpResponse<String> signInPage =
        browser.send(
            HttpRequest.newBuilder(url("/signin")).GET().build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    Matcher csrf = CSRF_FIELD.matcher(signInPage.body());
    assertThat(csrf.find()).as("the sign-in page carries a CSRF token").isTrue();

    String form =
        "_csrf=%s&j_username=%s&j_password=secret&j_password_2=secret"
            .formatted(encode(csrf.group(1)), encode(username));
    HttpResponse<Void> registered =
        browser.send(
            HttpRequest.newBuilder(url("/register"))
                // Exactly what a browser sends: the media type, and no charset.
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.US_ASCII))
                .build(),
            HttpResponse.BodyHandlers.discarding());

    assertThat(registered.statusCode()).isEqualTo(302);
    assertThat(identity.exists(username)).isTrue();
  }

  @Test
  @DisplayName("pages are sent as UTF-8 and say so")
  void pagesAreSentAsUtf8() throws IOException, InterruptedException {
    HttpResponse<String> home =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(url("/")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertThat(home.headers().firstValue("Content-Type").orElse(""))
        .isEqualToIgnoringCase("text/html;charset=UTF-8");
    assertThat(home.body()).contains("<meta charset=\"utf-8\" />");
  }

  private URI url(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
