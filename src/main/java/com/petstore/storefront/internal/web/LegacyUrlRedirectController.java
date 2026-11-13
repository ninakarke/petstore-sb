package com.petstore.storefront.internal.web;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Keeps links to the legacy application working.
 *
 * <p>Legacy page addresses ended in {@code .screen} and carried their subject in a query parameter,
 * for example {@code /petstore/category.screen?category_id=DOGS}. Each one is answered with a
 * permanent redirect to the page that replaced it. The table below is the whole mapping; every
 * migrated slice adds its screens to it.
 *
 * <p>The legacy application was deployed under the context path {@code /petstore}. Addresses are
 * accepted with and without that prefix.
 */
@Controller
class LegacyUrlRedirectController {

  /** Legacy screen name to the address of its replacement, computed from the query parameters. */
  private static final Map<String, Function<Map<String, String>, Optional<URI>>> SCREENS =
      Map.of(
          "main", parameters -> Optional.of(URI.create("/")),
          "category",
              parameters ->
                  subject(parameters, "category_id")
                      .map(id -> paged("/categories/{id}", id, parameters)),
          "product",
              parameters ->
                  subject(parameters, "product_id")
                      .map(id -> paged("/products/{id}", id, parameters)),
          "item",
              parameters ->
                  subject(parameters, "item_id")
                      .map(id -> UriComponentsBuilder.fromPath("/items/{id}").build(id)),
          "search",
              parameters -> {
                UriComponentsBuilder target =
                    UriComponentsBuilder.fromPath("/search").queryParam("keywords", "{keywords}");
                start(parameters).ifPresent(start -> target.queryParam("start", start));
                return Optional.of(target.build(parameters.getOrDefault("keywords", "")));
              });

  @GetMapping({"/{screen}.screen", "/petstore/{screen}.screen"})
  ResponseEntity<Void> screen(
      @PathVariable String screen, @RequestParam Map<String, String> parameters) {
    URI target =
        Optional.ofNullable(SCREENS.get(screen))
            .flatMap(route -> route.apply(parameters))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return movedPermanently(target);
  }

  /** The legacy welcome file and the bare legacy context path. */
  @GetMapping({"/index.jsp", "/petstore", "/petstore/", "/petstore/index.jsp"})
  ResponseEntity<Void> welcome() {
    return movedPermanently(URI.create("/"));
  }

  private static ResponseEntity<Void> movedPermanently(URI target) {
    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(target).build();
  }

  /** The id a legacy address is about; absent if the parameter is missing or blank. */
  private static Optional<String> subject(Map<String, String> parameters, String name) {
    return Optional.ofNullable(parameters.get(name)).filter(value -> !value.isBlank());
  }

  /** The legacy {@code start} parameter, if it is a positive number. The page size is dropped. */
  private static Optional<Integer> start(Map<String, String> parameters) {
    try {
      return Optional.ofNullable(parameters.get("start"))
          .map(Integer::parseInt)
          .filter(start -> start > 0);
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private static URI paged(String path, String id, Map<String, String> parameters) {
    UriComponentsBuilder target = UriComponentsBuilder.fromPath(path);
    start(parameters).ifPresent(start -> target.queryParam("start", start));
    return target.build(id);
  }
}
