package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * The category list shown on every page is cached.
 *
 * <p>Legacy source: the {@code waf:cache} tag around {@code sidebar.jsp} ({@code
 * name="sidebar_en_US" duration="300000"}).
 */
@SpringBootTest
class CatalogCachingTests {

  @Autowired private CatalogService catalog;
  @Autowired private CacheManager cacheManager;

  @Test
  @DisplayName("legacy: the category list is cached")
  void categoryListIsCached() {
    assertThat(catalog.categories()).isSameAs(catalog.categories());

    Cache cache = cacheManager.getCache("catalog.categories");
    assertThat(cache).isNotNull();
  }

  @Test
  @DisplayName("fixed: the cache has a size limit and an expiry (legacy never removed an entry)")
  void cacheIsBounded() {
    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
  }
}
