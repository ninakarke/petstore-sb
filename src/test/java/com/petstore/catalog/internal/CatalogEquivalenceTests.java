package com.petstore.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.catalog.Category;
import com.petstore.catalog.Item;
import com.petstore.catalog.Page;
import com.petstore.catalog.Product;
import com.petstore.shared.SeedProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Every catalog question the application can ask, asked of a real data store holding the complete
 * seed catalog, and compared answer by answer with a reference.
 *
 * <p>The reference is the in-memory store: a few lines of plain Java that state the rules directly
 * and pass the same contract suite. Subclasses choose the real store. Because every store is
 * compared with the same reference, all of them are shown to give the same pages, in the same
 * order, for the same data, and the relational ones are in turn compared with the legacy SQL by the
 * differential tests.
 *
 * <p>The contract suite checks each rule on a small fixture. This checks all rules together on all
 * the data, which is where an ordering or paging difference between stores would show.
 */
@SpringBootTest
abstract class CatalogEquivalenceTests {

  /** Legacy page size, and a second size so that page boundaries fall differently. */
  private static final int[] PAGE_SIZES = {2, 5};

  private static final List<Set<String>> SEARCHES =
      List.of(
          Set.of("fish"),
          Set.of("FISH"),
          Set.of("dog"),
          Set.of("Angel"),
          Set.of("a"),
          Set.of("e"),
          Set.of("male"),
          Set.of("great companion"),
          Set.of("koi", "parrot"),
          Set.of("%"),
          Set.of("_"),
          Set.of("."),
          Set.of("unicorn"));

  @Autowired private CatalogStore store;
  @Autowired private SeedProperties seed;

  @Test
  @DisplayName("every listing, page and search gives the same answer as the reference store")
  void storeAnswersLikeTheReference() {
    InMemoryCatalogStore reference = new InMemoryCatalogStore();
    new CatalogSeeder(reference, seed).run(null);

    List<String> expected = answers(reference);
    List<String> actual = answers(store);

    assertThat(expected).as("the reference holds the seed catalog").hasSizeGreaterThan(200);
    assertThat(actual).containsExactlyElementsOf(expected);
  }

  /** Asks every question and writes down every answer, one line each. */
  private static List<String> answers(CatalogStore store) {
    List<String> lines = new ArrayList<>();
    List<Category> categories = store.findCategories();
    lines.add("categories: " + categories);
    for (Category category : categories) {
      lines.add("category " + category.id() + ": " + store.findCategory(category.id()));
      for (int size : PAGE_SIZES) {
        String listing = "products of " + category.id() + " by " + size;
        List<Product> products =
            pages(lines, listing, size, start -> store.findProducts(category.id(), start, size));
        for (Product product : products) {
          lines.add("product " + product.id() + ": " + store.findProduct(product.id()));
          String itemListing = "items of " + product.id() + " by " + size;
          List<Item> items =
              pages(lines, itemListing, size, start -> store.findItems(product.id(), start, size));
          for (Item item : items) {
            lines.add("item " + item.itemId() + ": " + store.findItem(item.itemId()));
          }
        }
      }
    }
    for (Set<String> keywords : SEARCHES) {
      for (int size : PAGE_SIZES) {
        String listing = "search " + keywords.stream().sorted().toList() + " by " + size;
        pages(lines, listing, size, start -> store.searchItems(keywords, start, size));
      }
    }
    return lines;
  }

  /** Walks a listing page by page, as the "next" link does, and records each page. */
  private static <T> List<T> pages(
      List<String> lines, String listing, int size, IntFunction<Page<T>> read) {
    List<T> all = new ArrayList<>();
    int start = 0;
    while (true) {
      Page<T> page = read.apply(start);
      lines.add(listing + " from " + start + ": " + page);
      all.addAll(page.items());
      if (!page.hasNext()) {
        return all;
      }
      assertThat(page.size())
          .as("%s: a page that has a next page is full", listing)
          .isEqualTo(size);
      start = page.nextStart();
    }
  }
}
