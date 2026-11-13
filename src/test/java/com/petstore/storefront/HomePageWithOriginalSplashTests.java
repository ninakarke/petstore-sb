package com.petstore.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The home page when the original splash picture is present in the local picture folder.
 *
 * <p>Legacy source: {@code docroot/main.jsp}, an image map over {@code splash.gif}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HomePageWithOriginalSplashTests {

  @TempDir static Path localPictures;

  @DynamicPropertySource
  static void useFolderContainingTheSplashPicture(DynamicPropertyRegistry registry)
      throws IOException {
    Files.write(localPictures.resolve("splash.gif"), new byte[] {'G', 'I', 'F'});
    registry.add("petstore.images.local-directory", localPictures::toString);
  }

  @Autowired private MockMvcTester mvc;

  @Test
  @DisplayName(
      "legacy main.jsp: the picture's areas link to the categories, at the legacy coordinates")
  void homePageShowsTheImageMap() {
    assertThat(mvc.get().uri("/"))
        .hasStatusOk()
        .bodyText()
        .contains("src=\"/images/splash.gif\"")
        .contains("usemap=\"#petmap\"")
        .contains("href=\"/categories/FISH\" alt=\"Fish\" shape=\"rect\" coords=\"2,180,72,250\"")
        .contains(
            "href=\"/categories/CATS\" alt=\"Cats\" shape=\"rect\" coords=\"225,240,295,310\"")
        .doesNotContain("category-tiles");
  }
}
