package com.petstore.shared;

import java.util.Locale;

/**
 * The one language the store is offered in: US English.
 *
 * <p>The legacy application was offered in three languages and stored catalog text and prices once
 * per language, under a code in the {@code locale} column of the catalog tables. This application
 * is English only. The tables keep their legacy shape, so the code is still written to and read
 * from that column; it is always this one.
 */
public final class StoreLocale {

  /** Used for formatting, for example of prices. */
  public static final Locale LOCALE = Locale.US;

  /** The value of the legacy {@code locale} column for English data. */
  public static final String CODE = "en_US";

  private StoreLocale() {}
}
