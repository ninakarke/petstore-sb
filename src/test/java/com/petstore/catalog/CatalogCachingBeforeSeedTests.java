package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.internal.CatalogStore.CategoryText;
import com.petstore.catalog.internal.InMemoryCatalogStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * The cached category list and the moment before the seed data is loaded.
 *
 * <p>The application answers requests a fraction of a second before its seed loaders have run. A
 * request in that moment sees an empty catalog. Found by using the running application: the empty
 * list was cached, and the sidebar, the categories endpoint and the favourite category on the
 * account page stayed empty for the five minutes a cache entry lives.
 *
 * <p>The test puts the application in that moment on purpose: seed loading is switched off and the
 * catalog store starts empty.
 */
@SpringBootTest(properties = "petstore.seed.enabled=false")
@Import(CatalogCachingBeforeSeedTests.EmptyStore.class)
class CatalogCachingBeforeSeedTests {

  @TestConfiguration(proxyBeanMethods = false)
  static class EmptyStore {

    @Bean
    @Primary
    InMemoryCatalogStore emptyCatalogStore() {
      return new InMemoryCatalogStore();
    }
  }

  @Autowired private CatalogService catalog;
  @Autowired private InMemoryCatalogStore store;

  @Test
  @DisplayName("fixed: a category list read before the data is loaded is not kept in the cache")
  void emptyListIsNotCached() {
    // A request arrives before the seed loader has run.
    assertThat(catalog.categories()).isEmpty();

    // The seed loader runs.
    store.saveCategory("FISH", new CategoryText("Fish", "fish_icon.gif", null));

    // The next request sees the catalog at once, not after the cache entry would have expired.
    assertThat(catalog.categories()).extracting(Category::id).containsExactly("FISH");

    // From here on the list is cached as before: a later change is not seen until it expires.
    store.saveCategory("DOGS", new CategoryText("Dogs", "dogs_icon.gif", null));
    assertThat(catalog.categories()).extracting(Category::id).containsExactly("FISH");
  }
}
