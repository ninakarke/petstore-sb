package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Error pages, checked through a real HTTP server because error dispatching is done by the servlet
 * container.
 *
 * <p>The legacy {@code general_error.jsp} printed the exception and its stack trace to the browser.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ErrorPageTests {

  @LocalServerPort private int port;

  @Test
  void unknownPageShowsAFriendlyErrorPageWithoutInternals()
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/no-such-page"))
            .header("Accept", "text/html")
            .GET()
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body())
        .contains("The page you asked for does not exist.")
        .contains("class=\"banner\"")
        .doesNotContain("Exception")
        .doesNotContain("at com.petstore")
        .doesNotContain("at org.springframework");
  }
}
