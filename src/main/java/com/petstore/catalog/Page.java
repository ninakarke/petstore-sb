package com.petstore.catalog;

import java.util.List;

/**
 * One page of a longer list.
 *
 * <p>Legacy equivalent: {@code com.sun.j2ee.blueprints.catalog.model.Page}. As there, a page knows
 * where it starts and whether another page follows, but not how many results exist in total, so
 * navigation is "previous" and "next" only.
 *
 * @param items the results on this page
 * @param start zero-based position of the first result of this page in the full list
 * @param count the number of results that was asked for
 * @param hasNext whether at least one more result follows this page
 */
public record Page<T>(List<T> items, int start, int count, boolean hasNext) {

  public Page {
    items = List.copyOf(items);
  }

  public static <T> Page<T> empty() {
    return new Page<>(List.of(), 0, 0, false);
  }

  public int size() {
    return items.size();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public boolean hasPrevious() {
    return start > 0;
  }

  /** Start of the page after this one. */
  public int nextStart() {
    return start + items.size();
  }

  /**
   * Start of the page before this one.
   *
   * <p>The legacy class stepped back by the number of results on the <em>current</em> page. On a
   * short last page that landed in the middle of the previous page and showed a result twice. This
   * steps back by the page size that was asked for.
   */
  public int previousStart() {
    return Math.max(start - count, 0);
  }
}
