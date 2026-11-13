package com.petstore.catalog.internal.api;

import com.petstore.catalog.Page;
import java.util.List;

/**
 * One page of results as returned by the API.
 *
 * @param items the results on this page
 * @param start zero-based position of the first result in the full list
 * @param count the page size that was applied
 * @param hasPrevious whether an earlier page exists
 * @param hasNext whether a later page exists
 * @param previousStart the {@code start} to request for the earlier page
 * @param nextStart the {@code start} to request for the later page
 */
record PageResponse<T>(
    List<T> items,
    int start,
    int count,
    boolean hasPrevious,
    boolean hasNext,
    int previousStart,
    int nextStart) {

  static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.items(),
        page.start(),
        page.count(),
        page.hasPrevious(),
        page.hasNext(),
        page.previousStart(),
        page.nextStart());
  }
}
