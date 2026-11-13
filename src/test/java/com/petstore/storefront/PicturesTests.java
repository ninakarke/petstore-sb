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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Pictures: a file from the local folder when present, a placeholder otherwise.
 *
 * <p>The legacy application served its images from {@code docroot/images}. Their licence forbids
 * redistribution, so this repository ships placeholders and reads originals only from a local,
 * untracked folder.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PicturesTests {

  private static final byte[] FAKE_GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 2, 3};

  @TempDir static Path localPictures;

  @DynamicPropertySource
  static void useTemporaryPictureFolder(DynamicPropertyRegistry registry) throws IOException {
    Files.write(localPictures.resolve("fish3.gif"), FAKE_GIF);
    Files.writeString(localPictures.resolve(".secret"), "do not serve");
    registry.add("petstore.images.local-directory", localPictures::toString);
  }

  @Autowired private MockMvcTester mvc;

  @Test
  @DisplayName("a picture present in the local folder is served as it is")
  void localPictureIsServed() {
    assertThat(mvc.get().uri("/images/fish3.gif"))
        .hasStatusOk()
        .hasHeader("Content-Type", "image/gif")
        .body()
        .isEqualTo(FAKE_GIF);
  }

  @Test
  @DisplayName("a picture that is not in the local folder is replaced by a matching placeholder")
  void missingPictureGetsAPlaceholderOfTheRightKind() {
    assertThat(mvc.get().uri("/images/dog1.gif"))
        .hasStatusOk()
        .hasHeader("Content-Type", "image/svg+xml")
        .bodyText()
        .contains("<svg")
        .contains("Dog placeholder");
    assertThat(mvc.get().uri("/images/lizard2.gif")).bodyText().contains("Reptile placeholder");
    assertThat(mvc.get().uri("/images/birds_icon.gif")).bodyText().contains("Bird placeholder");
    assertThat(mvc.get().uri("/images/cat3.gif")).bodyText().contains("Cat placeholder");
    assertThat(mvc.get().uri("/images/fish1.jpg")).bodyText().contains("Fish placeholder");
    assertThat(mvc.get().uri("/images/banner_logo.gif")).bodyText().contains("Pet Store");
    assertThat(mvc.get().uri("/images/something_else.gif")).bodyText().contains("Pet placeholder");
  }

  @Test
  void picturesMayBeCachedByTheBrowser() {
    assertThat(mvc.get().uri("/images/dog1.gif")).hasHeader("Cache-Control", "max-age=3600");
  }

  @Test
  @DisplayName("only plain file names are accepted: no hidden files, no way out of the folder")
  void unsafeNamesAreNotFound() {
    assertThat(mvc.get().uri("/images/.secret")).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/images/..secret")).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/images/a..b..c")).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri("/images/name with space.gif")).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void itemPageShowsTheItemsPicture() {
    assertThat(mvc.get().uri("/items/EST-1"))
        .bodyText()
        .contains("src=\"/images/fish3.gif\"")
        .contains("alt=\"Large Angelfish\"");
  }

  @Test
  void bannerShowsTheLogo() {
    assertThat(mvc.get().uri("/"))
        .bodyText()
        .contains("src=\"/images/banner_logo.gif\"")
        .contains("alt=\"Pet Store\"");
  }

  @Test
  @DisplayName("without the original splash picture the home page shows a tile per category")
  void homePageShowsTilesWithoutTheSplashPicture() {
    assertThat(mvc.get().uri("/"))
        .bodyText()
        .contains("class=\"category-tiles\"")
        .contains("src=\"/images/fish_icon.gif\"")
        .doesNotContain("usemap");
  }
}
