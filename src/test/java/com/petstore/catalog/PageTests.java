package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Paging rules of the catalog.
 *
 * <p>Legacy source: {@code components/catalog/.../model/Page.java}.
 */
class PageTests {

  @Test
  @DisplayName("legacy: the first page has no previous page")
  void firstPageHasNoPrevious() {
    Page<String> page = new Page<>(List.of("a", "b"), 0, 2, true);

    assertThat(page.hasPrevious()).isFalse();
    assertThat(page.hasNext()).isTrue();
  }

  @Test
  @DisplayName("legacy: the next page starts after the last result of this page")
  void nextPageStartsAfterThisPage() {
    Page<String> page = new Page<>(List.of("a", "b"), 0, 2, true);

    assertThat(page.nextStart()).isEqualTo(2);
  }

  @Test
  @DisplayName("legacy: any page that does not start at zero has a previous page")
  void laterPageHasPrevious() {
    Page<String> page = new Page<>(List.of("c", "d"), 2, 2, false);

    assertThat(page.hasPrevious()).isTrue();
    assertThat(page.previousStart()).isZero();
  }

  @Test
  @DisplayName("fixed: stepping back from a short last page lands on the start of the page before")
  void previousPageOfShortLastPageIsNotShifted() {
    // Three results, two per page: the last page holds only the third result.
    Page<String> lastPage = new Page<>(List.of("c"), 2, 2, false);

    // The legacy class computed start - size = 1, showing "b" and "c": "c" appeared twice.
    assertThat(lastPage.previousStart()).isZero();
  }

  @Test
  @DisplayName("legacy: the start of the previous page never goes below zero")
  void previousStartIsNeverNegative() {
    Page<String> page = new Page<>(List.of("b", "c"), 1, 2, false);

    assertThat(page.previousStart()).isZero();
  }

  @Test
  @DisplayName("legacy: the empty page starts at zero and has no neighbours")
  void emptyPage() {
    Page<String> page = Page.empty();

    assertThat(page.isEmpty()).isTrue();
    assertThat(page.size()).isZero();
    assertThat(page.hasPrevious()).isFalse();
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  void pageContentCannotBeChangedAfterwards() {
    List<String> source = new java.util.ArrayList<>(List.of("a"));
    Page<String> page = new Page<>(source, 0, 2, false);

    source.add("b");

    assertThat(page.items()).containsExactly("a");
  }
}
