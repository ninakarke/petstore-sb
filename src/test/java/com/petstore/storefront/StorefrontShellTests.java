package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The web shell: shared layout, home page and the security baseline. */
@SpringBootTest
@AutoConfigureMockMvc
class StorefrontShellTests {

  @Autowired private MockMvcTester mvc;

  @Test
  void homePageIsRenderedInsideTheSharedLayout() {
    assertThat(mvc.get().uri("/"))
        .hasStatusOk()
        .hasViewName("home")
        .bodyText()
        .contains("<title>Welcome to the BluePrints Petstore</title>")
        .contains("class=\"banner\"")
        .contains("class=\"footer\"");
  }

  @Test
  void notesInTemplatesAreNotSentToTheBrowser() {
    assertThat(mvc.get().uri("/")).bodyText().doesNotContain("<!--").doesNotContain("Legacy");
  }

  @Test
  void stylesheetIsServed() {
    assertThat(mvc.get().uri("/css/petstore.css")).hasStatusOk();
  }

  /** The legacy application changed state through plain GET links and had no CSRF protection. */
  @Test
  void stateChangingRequestWithoutCsrfTokenIsRejected() {
    assertThat(mvc.post().uri("/")).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void stateChangingRequestWithCsrfTokenPassesTheSecurityFilter() {
    // No handler accepts POST / yet, so the request gets past security and is then refused.
    assertThat(mvc.post().uri("/").with(csrf())).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  void responsesCarryStandardSecurityHeaders() {
    assertThat(mvc.get().uri("/"))
        .hasHeader("X-Content-Type-Options", "nosniff")
        .hasHeader("X-Frame-Options", "DENY");
  }
}
