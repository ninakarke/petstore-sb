package com.petstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The supported languages and the codes their data is stored under.
 *
 * <p>Legacy sources: {@code web.xml} ({@code locales=en_US,ja_JP,zh_CN}, {@code
 * default_locale=en_US}) and the {@code locale} column of the catalog tables.
 */
class SupportedLocaleTests {

  @Test
  @DisplayName("legacy: data is stored under the codes en_US, ja_JP and zh_CN")
  void codesMatchTheLegacyLocaleColumn() {
    assertThat(SupportedLocale.values())
        .extracting(SupportedLocale::code)
        .containsExactly("en_US", "ja_JP", "zh_CN");
  }

  @Test
  @DisplayName("legacy: the default locale is en_US")
  void defaultIsUsEnglish() {
    assertThat(SupportedLocale.DEFAULT).isEqualTo(SupportedLocale.EN_US);
    assertThat(SupportedLocale.of(null)).isEqualTo(SupportedLocale.EN_US);
  }

  @Test
  void exactLocaleIsMatched() {
    assertThat(SupportedLocale.of(Locale.JAPAN)).isEqualTo(SupportedLocale.JA_JP);
    assertThat(SupportedLocale.of(Locale.CHINA)).isEqualTo(SupportedLocale.ZH_CN);
  }

  @Test
  void languageAloneIsEnoughToMatch() {
    assertThat(SupportedLocale.of(Locale.JAPANESE)).isEqualTo(SupportedLocale.JA_JP);
    assertThat(SupportedLocale.of(Locale.UK)).isEqualTo(SupportedLocale.EN_US);
  }

  @Test
  void unsupportedLanguageFallsBackToTheDefault() {
    assertThat(SupportedLocale.of(Locale.GERMANY)).isEqualTo(SupportedLocale.EN_US);
  }

  @Test
  void localeCanBeFoundByItsStoredCode() {
    assertThat(SupportedLocale.fromCode("zh_CN")).contains(SupportedLocale.ZH_CN);
    assertThat(SupportedLocale.fromCode("de_DE")).isEmpty();
    assertThat(SupportedLocale.JA_JP.locale()).isEqualTo(Locale.JAPAN);
  }
}
