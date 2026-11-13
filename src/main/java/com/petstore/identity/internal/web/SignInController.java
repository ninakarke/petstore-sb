package com.petstore.identity.internal.web;

import com.petstore.identity.IdentityService;
import com.petstore.identity.RegistrationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The sign-in page, registration of a new user, and the page shown after signing out.
 *
 * <p>Legacy equivalents: the {@code signon} screen ({@code signon.jsp}, with its two forms), the
 * {@code signon_error} and {@code duplicate_account} screens, {@code createuser.do} and the {@code
 * signoff} screen. Checking the user name and password itself is done by Spring Security on {@code
 * POST /signin}, which replaces {@code j_signon_check} of the legacy filter; the form field names
 * are unchanged.
 */
@Controller
class SignInController {

  /**
   * Where a newly registered user is sent to: the page for entering their account details, as in
   * the legacy flow from {@code createuser.do} to the {@code create_customer} screen.
   */
  static final String AFTER_REGISTRATION = "/account/new";

  private final IdentityService identity;
  private final SignInSupport signInSupport;

  SignInController(IdentityService identity, SignInSupport signInSupport) {
    this.identity = identity;
    this.signInSupport = signInSupport;
  }

  /** Legacy: {@code signon.screen}; with {@code ?error}, {@code signon_error.screen}. */
  @GetMapping("/signin")
  String signInPage(
      HttpServletRequest request,
      @CookieValue(name = RememberUsernameFilter.COOKIE_NAME, required = false) String remembered,
      Model model) {
    model.addAttribute("signInFailed", request.getParameterMap().containsKey("error"));
    model.addAttribute("rememberedUsername", RememberUsernameFilter.decode(remembered));
    return "identity/signin";
  }

  /** Legacy: {@code createuser.do}. */
  @PostMapping("/register")
  String register(
      @RequestParam(name = "j_username", required = false) String username,
      @RequestParam(name = "j_password", required = false) String password,
      @RequestParam(name = "j_password_2", required = false) String repeatedPassword,
      @CookieValue(name = RememberUsernameFilter.COOKIE_NAME, required = false) String remembered,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    try {
      identity.register(username, password, repeatedPassword);
    } catch (RegistrationException refused) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute("registrationProblem", refused.problem());
      model.addAttribute("attemptedUsername", username);
      model.addAttribute("signInFailed", false);
      model.addAttribute("rememberedUsername", RememberUsernameFilter.decode(remembered));
      return "identity/signin";
    }
    signInSupport.signIn(username, request, response);
    return "redirect:" + AFTER_REGISTRATION;
  }

  /** Legacy: {@code signoff.screen}. */
  @GetMapping("/signedout")
  String signedOutPage() {
    return "identity/signedout";
  }
}
