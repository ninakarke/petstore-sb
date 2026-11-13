package com.petstore.storefront.internal.web;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves pictures under {@code /images/}, the address the legacy pages used.
 *
 * <p>A file from the local folder is served if present, otherwise a placeholder. See {@link
 * Pictures}.
 */
@Controller
class ImageController {

  private static final MediaType SVG = MediaType.parseMediaType("image/svg+xml");
  private static final CacheControl ONE_HOUR = CacheControl.maxAge(Duration.ofHours(1));

  private final Pictures pictures;

  ImageController(Pictures pictures) {
    this.pictures = pictures;
  }

  @GetMapping("/images/{name}")
  ResponseEntity<Resource> image(@PathVariable String name) {
    if (!Pictures.isSafeName(name)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Optional<Path> local = pictures.local(name);
    if (local.isPresent()) {
      return ResponseEntity.ok()
          .contentType(
              MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM))
          .cacheControl(ONE_HOUR)
          .body(new FileSystemResource(local.get()));
    }
    return ResponseEntity.ok()
        .contentType(SVG)
        .cacheControl(ONE_HOUR)
        .body(new ClassPathResource("static/placeholders/" + Pictures.placeholder(name) + ".svg"));
  }
}
