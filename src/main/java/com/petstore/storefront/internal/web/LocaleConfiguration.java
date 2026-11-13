package com.petstore.storefront.internal.web;

import com.petstore.shared.StoreLocale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

/**
 * The store is offered in US English only.
 *
 * <p>Fixing the locale, instead of following the browser's language preference, makes sure that
 * texts and number formats agree: an English page never shows a price formatted for another
 * country. The legacy application could be switched between three languages; that is out of scope
 * here.
 */
@Configuration(proxyBeanMethods = false)
class LocaleConfiguration {

  @Bean
  LocaleResolver localeResolver() {
    return new FixedLocaleResolver(StoreLocale.LOCALE);
  }
}
