package com.petstore.storefront.internal.web;

import com.petstore.shared.SupportedLocale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * How the visitor's language is determined.
 *
 * <p>As in the legacy application, the language is kept in the session and starts as {@code en_US}
 * ({@code default_locale} in the legacy {@code web.xml}). The browser's language preference is not
 * consulted; the legacy application did not consult it either.
 */
@Configuration(proxyBeanMethods = false)
class LocaleConfiguration {

  @Bean
  LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(SupportedLocale.DEFAULT.locale());
    return resolver;
  }
}
