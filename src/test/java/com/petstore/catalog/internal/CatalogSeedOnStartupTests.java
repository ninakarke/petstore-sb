package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The running application starts with the legacy catalog loaded into its database. */
@SpringBootTest
class CatalogSeedOnStartupTests {

  @Autowired private CatalogStore store;

  @Test
  void catalogIsLoadedWhenTheApplicationStartsOnAnEmptyDatabase() {
    assertThat(store.isEmpty()).isFalse();
    assertThat(store.findCategories())
        .extracting(Category::name)
        .containsExactly("Birds", "Cats", "Dogs", "Fish", "Reptiles");
    assertThat(store.findItem("EST-1")).isPresent();
  }
}
