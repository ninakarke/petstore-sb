package com.petstore.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Signs a user in without the sign-in form, right after they have created their account.
 *
 * <p>The legacy application did the same at the end of registration by setting the session
 * attribute {@code j_signon} to {@code true}.
 */
@Component
class SignInSupport {

  private final UserDetailsService users;
  private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

  SignInSupport(UserDetailsService users) {
    this.users = users;
  }

  void signIn(String username, HttpServletRequest request, HttpServletResponse response) {
    UserDetails user = users.loadUserByUsername(username);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    SecurityContextHolder.setContext(context);
    // A new session id for the signed-in session, keeping what is in it (such as the cart).
    request.getSession(true);
    request.changeSessionId();
    contexts.saveContext(context, request, response);
  }
}
