package com.petstore.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Signing out ends the session but keeps the visitor's language.
 *
 * <p>Same behaviour as the legacy {@code SignOffHTMLAction}: it read the locale from the session,
 * invalidated the session, created a new one and put the locale back. Everything else in the
 * session, including the cart, is gone, as it was there.
 */
public class KeepLanguageOnSignOut implements LogoutHandler, LogoutSuccessHandler {

  private static final String LANGUAGE = SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME;
  private static final String CARRIED_LANGUAGE =
      KeepLanguageOnSignOut.class.getName() + ".language";

  private final String signedOutPath;

  public KeepLanguageOnSignOut(String signedOutPath) {
    this.signedOutPath = signedOutPath;
  }

  /** Runs before the session is invalidated: remember the language for this request. */
  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      request.setAttribute(CARRIED_LANGUAGE, session.getAttribute(LANGUAGE));
    }
  }

  /** Runs after the session is invalidated: put the language into the new session. */
  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    Object language = request.getAttribute(CARRIED_LANGUAGE);
    if (language != null) {
      request.getSession(true).setAttribute(LANGUAGE, language);
    }
    response.sendRedirect(request.getContextPath() + signedOutPath);
  }
}
