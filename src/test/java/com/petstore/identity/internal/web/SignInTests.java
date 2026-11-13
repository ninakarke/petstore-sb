package com.petstore.identity.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

import com.petstore.identity.IdentityService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Signing in, signing out, protected pages and creating a user, against the running application
 * with the legacy demo users.
 *
 * <p>Legacy sources: {@code components/signon/.../web/SignOnFilter.java}, {@code
 * docroot/WEB-INF/signon-config.xml}, {@code docroot/signon.jsp}, {@code signon_failed.jsp}, {@code
 * signoff.jsp}, {@code duplicate_account.jsp}, {@code SignOffHTMLAction} and {@code
 * CreateUserHTMLAction}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SignInTests {

  @Autowired private MockMvcTester mvc;
  @Autowired private IdentityService identity;

  private MockHttpSession session;

  @BeforeEach
  void newBrowserSession() {
    session = new MockHttpSession();
  }

  // ---- sign-in page ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "legacy signon.jsp: a sign-in form and a create-account form, with the legacy field names")
  void signInPageHasBothForms() {
    assertThat(mvc.get().uri("/signin"))
        .hasStatusOk()
        .hasViewName("identity/signin")
        .bodyText()
        .contains("<title>Sign On</title>")
        .contains("Are you a returning customer?")
        .contains("action=\"/signin\" method=\"post\"")
        .contains("name=\"j_username\"")
        .contains("name=\"j_password\"")
        .contains("name=\"j_remember_username\"")
        .contains("Remember My User Name")
        .contains("No. I would like to sign up for an account.")
        .contains("action=\"/register\" method=\"post\"")
        .contains("name=\"j_password_2\"")
        .contains("Create New Account");
  }

  @Test
  @DisplayName("fixed: the form is not prefilled with the demo user name and password")
  void signInPageIsNotPrefilledWithDemoCredentials() {
    assertThat(mvc.get().uri("/signin")).bodyText().doesNotContain("value=\"j2ee\"");
  }

  // ---- signing in -----------------------------------------------------------------------------

  @Test
  @DisplayName("legacy: the right user name and password sign the user in")
  void correctCredentialsSignIn() {
    assertThat(signIn("j2ee", "j2ee"))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/")
        .matches(authenticated().withUsername("j2ee").withRoles("CUSTOMER"));
  }

  @Test
  @DisplayName("legacy: a wrong password leads to the sign-in error message")
  void wrongPasswordIsRejected() {
    assertThat(signIn("j2ee", "wrong"))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/signin?error")
        .matches(unauthenticated());

    assertThat(mvc.get().uri("/signin?error"))
        .bodyText()
        .contains("Sign-in Error")
        .contains("The user name and password you entered were not found in our records.");
  }

  @Test
  @DisplayName("legacy: an unknown user gets the same answer as a wrong password")
  void unknownUserIsRejected() {
    assertThat(signIn("nobody", "j2ee"))
        .hasRedirectedUrl("/signin?error")
        .matches(unauthenticated());
  }

  @Test
  @DisplayName("legacy: the password is compared exactly, including case")
  void passwordIsCaseSensitive() {
    assertThat(signIn("j2ee", "J2EE")).matches(unauthenticated());
  }

  @Test
  @DisplayName("fixed: signing in needs a CSRF token")
  void signInWithoutCsrfTokenIsRefused() {
    assertThat(
            mvc.post()
                .uri("/signin")
                .param("j_username", "j2ee")
                .param("j_password", "j2ee")
                .session(session))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  // ---- protected pages ------------------------------------------------------------------------

  @Test
  @DisplayName("legacy signon-config.xml: account and order pages need a signed-in customer")
  void protectedPagesSendAnonymousVisitorsToTheSignInPage() {
    for (String url : new String[] {"/account", "/account/edit", "/checkout", "/orders/10011"}) {
      MvcTestResult result = mvc.get().uri(url).exchange();

      assertThat(result).as(url).hasStatus(HttpStatus.FOUND);
      assertThat(result.getResponse().getRedirectedUrl()).as(url).endsWith("/signin");
    }
  }

  @Test
  @DisplayName("fixed: placing an order needs a signed-in customer (legacy left order.do open)")
  void placingAnOrderIsProtected() {
    MvcTestResult result = mvc.post().uri("/orders").with(csrf()).exchange();

    assertThat(result.getResponse().getRedirectedUrl()).endsWith("/signin");
  }

  @Test
  @DisplayName("legacy: catalog, cart and sign-in pages are public")
  void publicPagesNeedNoSignIn() {
    for (String url :
        new String[] {
          "/",
          "/categories/FISH",
          "/items/EST-1",
          "/search?keywords=dog",
          "/cart",
          "/signin",
          "/signedout",
          "/images/fish3.gif",
          "/css/petstore.css",
          "/api/v1/catalog/categories",
          "/actuator/health"
        }) {
      assertThat(mvc.get().uri(url)).as(url).hasStatusOk();
    }
  }

  @Test
  @DisplayName(
      "legacy j_signon_original_url: after signing in the visitor lands on the page they asked for")
  void signingInReturnsToTheRequestedPage() {
    mvc.get().uri("/account").session(session).exchange();

    MvcTestResult result = signIn("j2ee", "j2ee");

    assertThat(result.getResponse().getRedirectedUrl()).contains("/account");
  }

  @Test
  void signedInCustomerPassesTheProtection() {
    // No page is mapped there yet, so getting past security shows as "not found", not a redirect.
    assertThat(mvc.get().uri("/orders/10011").with(user("j2ee").roles("CUSTOMER")))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  // ---- remember my user name --------------------------------------------------------------------

  @Test
  @DisplayName(
      "legacy: ticking Remember My User Name stores the user name in bp_signon for 31 days")
  void rememberedUserNameIsStoredInACookie() {
    MvcTestResult result =
        mvc.post()
            .uri("/signin")
            .param("j_username", "j2ee")
            .param("j_password", "j2ee")
            .param("j_remember_username", "on")
            .session(session)
            .with(csrf())
            .exchange();

    assertThat(result.getResponse().getHeader("Set-Cookie"))
        .contains("bp_signon=j2ee")
        .contains("Max-Age=2678400")
        .contains("HttpOnly");
  }

  @Test
  @DisplayName("legacy: without the tick the cookie is removed")
  void cookieIsRemovedWhenTheBoxIsNotTicked() {
    assertThat(signIn("j2ee", "j2ee").getResponse().getHeader("Set-Cookie"))
        .contains("bp_signon=")
        .contains("Max-Age=0");
  }

  @Test
  @DisplayName("legacy signon.jsp: a remembered user name is filled in and the box is ticked")
  void rememberedUserNameIsFilledIn() {
    assertThat(mvc.get().uri("/signin").cookie(new Cookie("bp_signon", "j2ee-ja")))
        .bodyText()
        .contains("value=\"j2ee-ja\"")
        .contains("checked=\"checked\"");
    assertThat(mvc.get().uri("/signin")).bodyText().doesNotContain("checked=\"checked\"");
  }

  @Test
  void rememberedUserNameSurvivesSpecialCharacters() {
    assertThat(RememberUsernameFilter.decode(RememberUsernameFilter.encode("düke; x")))
        .isEqualTo("düke; x");
    assertThat(RememberUsernameFilter.decode(null)).isEmpty();
  }

  // ---- signing out ----------------------------------------------------------------------------

  @Test
  @DisplayName("legacy signoff: the session ends and the signed-out page is shown")
  void signingOutEndsTheSession() {
    signIn("j2ee", "j2ee");

    assertThat(mvc.post().uri("/signout").session(session).with(csrf()))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/signedout")
        .matches(unauthenticated());
    assertThat(session.isInvalid()).isTrue();

    assertThat(mvc.get().uri("/signedout"))
        .hasViewName("identity/signedout")
        .bodyText()
        .contains("<title>Sign Off</title>")
        .contains("You are signed out.")
        .contains("Thank you for shopping.")
        .contains("href=\"/signin\"");
  }

  @Test
  @DisplayName(
      "fixed: signing out needs a POST with a CSRF token (legacy signed out on a GET link)")
  void signingOutCannotBeTriggeredByALink() {
    signIn("j2ee", "j2ee");

    assertThat(mvc.post().uri("/signout").session(session)).hasStatus(HttpStatus.FORBIDDEN);
    mvc.get().uri("/signout").session(session).exchange();

    assertThat(session.isInvalid()).isFalse();
  }

  @Test
  @DisplayName("legacy: the cart is kept when signing in")
  void cartSurvivesSigningIn() {
    mvc.post().uri("/cart/items").param("itemId", "EST-1").session(session).with(csrf()).exchange();

    signIn("j2ee", "j2ee");

    assertThat(mvc.get().uri("/cart").session(session)).bodyText().contains("itemQuantity_EST-1");
  }

  // ---- banner ---------------------------------------------------------------------------------

  @Test
  @DisplayName("legacy banner.jsp: Sign in for visitors, Sign out for signed-in customers")
  void bannerOffersSignInOrSignOut() {
    assertThat(mvc.get().uri("/"))
        .bodyText()
        .contains("href=\"/signin\">Sign in</a>")
        .doesNotContain("Sign out");
    assertThat(mvc.get().uri("/").with(user("j2ee").roles("CUSTOMER")))
        .bodyText()
        .contains("action=\"/signout\" method=\"post\"")
        .contains(">Sign out</button>")
        .doesNotContain(">Sign in</a>");
  }

  // ---- creating a user ------------------------------------------------------------------------

  @Test
  @DisplayName("legacy createuser.do: a new user is created and signed in")
  void newUserIsCreatedAndSignedIn() {
    assertThat(register("newduke", "secret", "secret"))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl(SignInController.AFTER_REGISTRATION)
        .matches(authenticated().withUsername("newduke"));
    assertThat(identity.exists("newduke")).isTrue();

    // The new user can sign in with the password they chose.
    session = new MockHttpSession();
    assertThat(signIn("newduke", "secret")).matches(authenticated().withUsername("newduke"));
  }

  @Test
  @DisplayName("legacy duplicate_account.jsp: a taken user name is refused with the legacy message")
  void takenUserNameIsRefused() {
    assertThat(register("j2ee", "secret", "secret"))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasViewName("identity/signin")
        .matches(unauthenticated())
        .bodyText()
        .contains("User Creation Error!")
        .contains("The User name you have chosen is in use. Please choose another username.");
  }

  @Test
  @DisplayName("fixed: differing passwords are refused (legacy never compared them)")
  void differingPasswordsAreRefused() {
    assertThat(register("otherduke", "secret", "secrte"))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyText()
        .contains("The two passwords are not the same.")
        .contains("value=\"otherduke\"");
    assertThat(identity.exists("otherduke")).isFalse();
  }

  @Test
  @DisplayName("legacy rules are reported to the user")
  void legacyRulesAreReported() {
    assertThat(register("u".repeat(26), "secret", "secret"))
        .bodyText()
        .contains("The user name cannot be more than 25 characters long.");
    assertThat(register("du%ke", "secret", "secret"))
        .bodyText()
        .contains("The user name cannot contain the characters % or *.");
    assertThat(register("", "secret", "secret")).bodyText().contains("Please enter a user name.");
  }

  @Test
  void registrationNeedsACsrfToken() {
    assertThat(
            mvc.post()
                .uri("/register")
                .param("j_username", "sneaky")
                .param("j_password", "secret")
                .param("j_password_2", "secret"))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(identity.exists("sneaky")).isFalse();
  }

  private MvcTestResult signIn(String username, String password) {
    return mvc.post()
        .uri("/signin")
        .param("j_username", username)
        .param("j_password", password)
        .session(session)
        .with(csrf())
        .exchange();
  }

  private MvcTestResult register(String username, String password, String repeated) {
    return mvc.post()
        .uri("/register")
        .param("j_username", username)
        .param("j_password", password)
        .param("j_password_2", repeated)
        .session(session)
        .with(csrf())
        .exchange();
  }
}
