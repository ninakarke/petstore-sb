package com.petstore.shared;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The languages the store is offered in.
 *
 * <p>The legacy application listed them in the {@code locales} init parameter of its template
 * servlet ({@code en_US,ja_JP,zh_CN}) and stored catalog text and prices under the same codes in
 * the {@code locale} column of the catalog tables. The codes are kept unchanged so that existing
 * data stays valid.
 */
public enum SupportedLocale {
  EN_US(Locale.US),
  JA_JP(Locale.JAPAN),
  ZH_CN(Locale.CHINA);

  /** The legacy application's {@code default_locale}. */
  public static final SupportedLocale DEFAULT = EN_US;

  private final Locale locale;

  SupportedLocale(Locale locale) {
    this.locale = locale;
  }

  public Locale locale() {
    return locale;
  }

  /** The code under which data for this language is stored, for example {@code en_US}. */
  public String code() {
    return locale.toString();
  }

  /** Finds a supported locale by its stored code. */
  public static Optional<SupportedLocale> fromCode(String code) {
    return Arrays.stream(values()).filter(candidate -> candidate.code().equals(code)).findFirst();
  }

  /**
   * Maps any locale to a supported one: an exact match on language and country, otherwise a match
   * on language alone, otherwise the default.
   */
  public static SupportedLocale of(Locale requested) {
    if (requested == null) {
      return DEFAULT;
    }
    return Arrays.stream(values())
        .filter(candidate -> candidate.locale.equals(requested))
        .findFirst()
        .or(() ->
            Arrays.stream(values())
                .filter(candidate -> candidate.locale.getLanguage().equals(requested.getLanguage()))
                .findFirst())
        .orElse(DEFAULT);
  }
}
