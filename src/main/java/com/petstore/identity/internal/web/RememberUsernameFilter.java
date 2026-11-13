package com.petstore.identity.internal.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * "Remember My User Name" on the sign-in form.
 *
 * <p>Same behaviour as the legacy {@code SignOnFilter.validateSignOn}: when the box is ticked, the
 * user name is kept in the cookie {@code bp_signon} for 31 days, whether or not the sign-in
 * succeeds; when it is not ticked, the cookie is removed. Only the user name is remembered, never
 * the password, and the cookie does not sign anyone in.
 */
public class RememberUsernameFilter extends OncePerRequestFilter {

  /** Legacy: {@code SignOnFilter.COOKIE_NAME}. */
  public static final String COOKIE_NAME = "bp_signon";

  /** Legacy: {@code SignOnFilter.REMEMBER_USERNAME}. */
  public static final String REMEMBER_PARAMETER = "j_remember_username";

  /** Legacy: {@code userNameCookie.setMaxAge(2678400)}, 31 days. */
  static final Duration LIFETIME = Duration.ofSeconds(2_678_400);

  private final String signInPath;
  private final String usernameParameter;

  public RememberUsernameFilter(String signInPath, String usernameParameter) {
    this.signInPath = signInPath;
    this.usernameParameter = usernameParameter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if ("POST".equals(request.getMethod()) && signInPath.equals(pathOf(request))) {
      String username = request.getParameter(usernameParameter);
      boolean remember = request.getParameter(REMEMBER_PARAMETER) != null && username != null;
      response.addHeader(
          HttpHeaders.SET_COOKIE,
          ResponseCookie.from(COOKIE_NAME, remember ? encode(username) : "")
              .maxAge(remember ? LIFETIME : Duration.ZERO)
              .path("/")
              .httpOnly(true)
              .sameSite("Lax")
              .build()
              .toString());
    }
    chain.doFilter(request, response);
  }

  /** The request path inside the application, however the servlet happens to be mapped. */
  private static String pathOf(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }

  static String encode(String username) {
    return URLEncoder.encode(username, StandardCharsets.UTF_8);
  }

  public static String decode(String cookieValue) {
    return cookieValue == null ? "" : URLDecoder.decode(cookieValue, StandardCharsets.UTF_8);
  }
}
