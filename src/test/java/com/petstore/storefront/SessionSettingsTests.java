package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Session settings, checked on a real server.
 *
 * <p>Legacy source: {@code <session-timeout>15</session-timeout>} in {@code WEB-INF/web.xml}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SessionSettingsTests {

  @LocalServerPort private int port;
  @Autowired private ServletContext servletContext;

  @Test
  @DisplayName("legacy: a session, and with it the cart, ends after 15 idle minutes")
  void sessionTimeoutIsFifteenMinutes() {
    assertThat(servletContext.getSessionTimeout()).isEqualTo(15);
  }

  @Test
  @DisplayName("fixed: the session cookie cannot be read by scripts and is not sent cross-site")
  void sessionCookieIsProtected() throws IOException, InterruptedException {
    // Adding to the cart needs a session; fetch a page first to obtain the CSRF token and cookie.
    HttpResponse<String> page =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/items/EST-1"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    String cookie = page.headers().firstValue("Set-Cookie").orElse("");

    assertThat(cookie).contains("JSESSIONID=").contains("HttpOnly").contains("SameSite=Lax");
  }
}
