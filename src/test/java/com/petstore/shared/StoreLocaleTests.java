package com.petstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The store's one language.
 *
 * <p>Legacy sources: {@code web.xml} ({@code default_locale=en_US}) and the {@code locale} column
 * of the catalog tables.
 */
class StoreLocaleTests {

  @Test
  @DisplayName("legacy: English data is stored under the code en_US")
  void codeMatchesTheLegacyLocaleColumn() {
    assertThat(StoreLocale.CODE).isEqualTo("en_US");
    assertThat(StoreLocale.LOCALE).isEqualTo(Locale.US);
    assertThat(StoreLocale.LOCALE.toString()).isEqualTo(StoreLocale.CODE);
  }
}
