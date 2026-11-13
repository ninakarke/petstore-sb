package com.petstore.customer.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.petstore.customer.Customer;
import com.petstore.customer.CustomerFixtures;
import com.petstore.customer.CustomerService;
import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The account pages, against the running application and the legacy demo customers.
 *
 * <p>Legacy sources: {@code docroot/customer.jsp}, {@code create_customer.jsp}, {@code
 * edit_customer.jsp}, {@code CustomerHTMLAction}, {@code CustomerEJBAction} and {@code
 * CreateUserFlowHandler}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountPagesTests {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private MockMvcTester mvc;
  @Autowired private CustomerService customers;

  // ---- viewing ---------------------------------------------------------------------------------

  @Test
  @DisplayName("legacy customer.jsp: the account page shows contact details, card and profile")
  void accountPageShowsTheCustomer() {
    assertThat(mvc.get().uri("/account").with(user("j2ee")))
        .hasStatusOk()
        .hasViewName("customer/account")
        .bodyText()
        .contains("<title>Customer</title>")
        .contains("Your Account Information")
        .contains("Contact Information")
        .contains("<dd>XYZ</dd>")
        .contains("<dd>ABC</dd>")
        .contains("1234 Anywhere Street")
        .contains("Unit 555")
        .contains("<dd>Palo Alto</dd>")
        .contains("<dd>CA</dd>")
        .contains("<dd>94303</dd>")
        .contains("<dd>aaa@bbb.ccc</dd>")
        .contains("Credit Card Information")
        .contains("<dd>Meow Card</dd>")
        .contains("<span>04</span> / <span>04</span>")
        .contains("Profile Information")
        .contains("<b>Reptiles</b>")
        .contains("href=\"/account/edit\">Edit Your Account Information</a>");
  }

  @Test
  @DisplayName("fixed: only the last four characters of the card number are shown")
  void cardNumberIsMaskedOnTheAccountPage() {
    assertThat(mvc.get().uri("/account").with(user("j2ee")))
        .bodyText()
        .contains("*****6789")
        .doesNotContain("123456789");
  }

  @Test
  @DisplayName("legacy customer.jsp: preferences are shown as Yes or No")
  void preferencesAreShownAsYesOrNo() {
    // Seed customer j2ee: my list on, banner off.
    assertThat(mvc.get().uri("/account").with(user("j2ee")))
        .bodyText()
        .containsSubsequence(
            "<b>Yes</b>", "I want to enable the MyList feature.",
            "<b>No</b>", "I want to enable the Pet Tips banners.");
  }

  @Test
  void signedInUserWithoutACustomerIsSentToTheCreateForm() {
    assertThat(mvc.get().uri("/account").with(user(newUser())))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/account/new");
    assertThat(mvc.get().uri("/account/edit").with(user(newUser())))
        .hasRedirectedUrl("/account/new");
  }

  // ---- creating --------------------------------------------------------------------------------

  @Test
  @DisplayName("legacy create_customer.jsp: the form asks for contact details, card and profile")
  void createFormHasAllFields() {
    assertThat(mvc.get().uri("/account/new").with(user(newUser())))
        .hasStatusOk()
        .hasViewName("customer/account-form")
        .bodyText()
        .contains("<title>Create Customer</title>")
        .contains("action=\"/account/new\" method=\"post\"")
        .contains("name=\"givenName\"")
        .contains("name=\"familyName\"")
        .contains("name=\"address1\"")
        .contains("name=\"address2\"")
        .contains("name=\"city\"")
        .contains("name=\"state\"")
        .contains("name=\"postalCode\"")
        .contains("name=\"country\"")
        .contains("name=\"telephone\"")
        .contains("name=\"email\"")
        .contains("name=\"cardNumber\"")
        .contains("name=\"cardType\"")
        .contains("name=\"expiryMonth\"")
        .contains("name=\"expiryYear\"")
        .contains("name=\"favoriteCategory\"")
        .contains("name=\"myList\"")
        .contains("name=\"banners\"")
        .contains("<option value=\"USA\">United States</option>")
        .contains("<option value=\"REPTILES\">Reptiles</option>")
        .contains(">Submit</button>");
  }

  @Test
  @DisplayName("fixed: the create form is not prefilled with demo data and offers current years")
  void createFormIsEmptyAndOffersCurrentYears() {
    assertThat(mvc.get().uri("/account/new").with(user(newUser())))
        .bodyText()
        .doesNotContain("BluesPrints")
        .doesNotContain("value=\"Duke\"")
        .doesNotContain("1234 Moon Way")
        .doesNotContain(">2001<")
        .contains("<option value=\"" + Year.now() + "\">" + Year.now() + "</option>");
  }

  @Test
  @DisplayName("the store is English only: the form has no language choice")
  void formHasNoLanguageChoice() {
    assertThat(mvc.get().uri("/account/new").with(user(newUser())))
        .bodyText()
        .doesNotContain("name=\"language\"")
        .doesNotContain("Japanese");
  }

  @Test
  @DisplayName("legacy createcustomer.do: the customer is created with an active account")
  void customerIsCreated() {
    String username = newUser();

    assertThat(validForm(mvc.post().uri("/account/new"), username))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/account");

    Customer created = customers.find(username).orElseThrow();
    assertThat(created.account().status()).isEqualTo("active");
    assertThat(created.account().contactInfo().givenName()).isEqualTo("Duke");
    assertThat(created.account().contactInfo().address().streetName2()).isNull();
    assertThat(created.account().creditCard().cardNumber()).isEqualTo("0100-001-0001");
    assertThat(created.account().creditCard().expiryDate()).isEqualTo("10/2031");
    assertThat(created.profile().favoriteCategory()).isEqualTo("DOGS");
    assertThat(created.profile().myListPreference()).isTrue();
    assertThat(created.profile().bannerPreference()).isFalse();
    assertThat(created.profile().preferredLanguage()).isEqualTo("en_US");
  }

  @Test
  @DisplayName(
      "fixed: missing required fields are reported on the form (legacy checked in the browser only)")
  void missingFieldsAreReported() {
    String username = newUser();

    assertThat(
            mvc.post()
                .uri("/account/new")
                .param("givenName", "Duke")
                .param("cardType", "Duke Express")
                .param("expiryMonth", "10")
                .param("expiryYear", "2031")
                .param("favoriteCategory", "DOGS")
                .with(user(username))
                .with(csrf()))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasViewName("customer/account-form")
        .bodyText()
        .contains("Please complete the following:")
        .contains("<li>Last Name</li>")
        .contains("<li>Street Address</li>")
        .contains("<li>City</li>")
        .contains("<li>Telephone Number</li>")
        .contains("<li>Credit Card Number</li>")
        .doesNotContain("<li>First Name</li>")
        .contains("value=\"Duke\"");
    assertThat(customers.exists(username)).isFalse();
  }

  @Test
  void userWhoAlreadyHasACustomerCannotCreateAnother() {
    assertThat(mvc.get().uri("/account/new").with(user("j2ee"))).hasRedirectedUrl("/account");
    assertThat(validForm(mvc.post().uri("/account/new"), "j2ee")).hasRedirectedUrl("/account");
    assertThat(customers.find("j2ee").orElseThrow().account().contactInfo().givenName())
        .isEqualTo("XYZ");
  }

  // ---- editing ---------------------------------------------------------------------------------

  @Test
  @DisplayName("legacy edit_customer.jsp: the edit form shows the stored details")
  void editFormShowsTheStoredDetails() {
    assertThat(mvc.get().uri("/account/edit").with(user("j2ee")))
        .hasStatusOk()
        .bodyText()
        .contains("<title>Update Account</title>")
        .contains("action=\"/account\" method=\"post\"")
        .contains("value=\"XYZ\"")
        .contains("value=\"1234 Anywhere Street\"")
        .contains("value=\"Unit 555\"")
        .contains("<option value=\"REPTILES\" selected=\"selected\">Reptiles</option>");
  }

  @Test
  @DisplayName("fixed: a stored value that is not among the choices is kept, not silently replaced")
  void storedValuesOutsideTheChoicesAreKept() {
    // Seed customer j2ee has state "CA" and expiry year "04"; the lists offer neither.
    assertThat(mvc.get().uri("/account/edit").with(user("j2ee")))
        .bodyText()
        .contains("<option value=\"CA\" selected=\"selected\">CA</option>")
        .contains("<option value=\"04\" selected=\"selected\">04</option>");
  }

  @Test
  @DisplayName("fixed: the stored card number is never sent back to the browser")
  void editFormDoesNotRevealTheCardNumber() {
    assertThat(mvc.get().uri("/account/edit").with(user("j2ee")))
        .bodyText()
        .doesNotContain("123456789")
        .contains("placeholder=\"*****6789\"")
        .contains("Leave the card number empty to keep the card on file.");
  }

  @Test
  @DisplayName("legacy customer.do?action=update: the details are replaced")
  void customerIsUpdated() {
    String username = existingCustomer();

    assertThat(
            mvc.post()
                .uri("/account")
                .param("givenName", "Changed")
                .param("familyName", "Name")
                .param("address1", "1 New Road")
                .param("address2", "Floor 2")
                .param("city", "Newtown")
                .param("state", "New York")
                .param("postalCode", "10002")
                .param("country", "Canada")
                .param("telephone", "111-111-1111")
                .param("email", "new@example.com")
                .param("cardNumber", "9999-8888")
                .param("cardType", "Java(TM) Card")
                .param("expiryMonth", "01")
                .param("expiryYear", "2032")
                .param("favoriteCategory", "CATS")
                .param("banners", "on")
                .with(user(username))
                .with(csrf()))
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/account");

    Customer updated = customers.find(username).orElseThrow();
    assertThat(updated.account().contactInfo().givenName()).isEqualTo("Changed");
    assertThat(updated.account().contactInfo().address().city()).isEqualTo("Newtown");
    assertThat(updated.account().contactInfo().address().country()).isEqualTo("Canada");
    assertThat(updated.account().creditCard().cardNumber()).isEqualTo("9999-8888");
    assertThat(updated.account().creditCard().expiryDate()).isEqualTo("01/2032");
    assertThat(updated.profile().favoriteCategory()).isEqualTo("CATS");
    assertThat(updated.profile().myListPreference()).isFalse();
    assertThat(updated.profile().bannerPreference()).isTrue();
  }

  @Test
  @DisplayName("fixed: leaving the card number empty keeps the card on file")
  void emptyCardNumberKeepsTheStoredOne() {
    String username = existingCustomer();
    String storedNumber =
        customers.find(username).orElseThrow().account().creditCard().cardNumber();

    assertThat(validForm(mvc.post().uri("/account"), username, "")).hasRedirectedUrl("/account");

    assertThat(customers.find(username).orElseThrow().account().creditCard().cardNumber())
        .isEqualTo(storedNumber);
  }

  @Test
  void invalidUpdateIsReportedAndChangesNothing() {
    String username = existingCustomer();

    assertThat(mvc.post().uri("/account").param("givenName", "").with(user(username)).with(csrf()))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyText()
        .contains("Please complete the following:")
        .contains("<li>First Name</li>");
    assertThat(customers.find(username).orElseThrow().account().contactInfo().givenName())
        .isEqualTo("XYZ");
  }

  // ---- protection ------------------------------------------------------------------------------

  @Test
  void accountChangesNeedACsrfToken() {
    assertThat(mvc.post().uri("/account").param("givenName", "X").with(user("j2ee")))
        .hasStatus(HttpStatus.FORBIDDEN);
    assertThat(mvc.post().uri("/account/new").param("givenName", "X").with(user(newUser())))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("legacy banner.jsp: every page links to the account")
  void bannerLinksToTheAccount() {
    assertThat(mvc.get().uri("/")).bodyText().contains("href=\"/account\">Account</a>");
  }

  // ---- the registration flow
  // ---------------------------------------------------------------------

  @Test
  @DisplayName("legacy flow: create a user, enter account details, arrive at the account")
  void newUserRegistersAndCompletesTheirAccount() {
    MockHttpSession session = new MockHttpSession();
    String username = newUser();

    assertThat(register(session, username)).hasRedirectedUrl("/account/new");
    assertThat(mvc.get().uri("/account/new").session(session)).hasStatusOk();
    assertThat(
            validFields(mvc.post().uri("/account/new"), "0100-001-0001")
                .session(session)
                .with(csrf()))
        .hasRedirectedUrl("/account");
    assertThat(mvc.get().uri("/account").session(session))
        .hasStatusOk()
        .bodyText()
        .contains("<dd>Duke</dd>");
  }

  @Test
  @DisplayName(
      "legacy CreateUserFlowHandler: after registering, the visitor returns to the page they asked for")
  void registrationReturnsToTheInterruptedPage() {
    MockHttpSession session = new MockHttpSession();

    // An anonymous visitor asks for a protected page and is sent to sign in.
    mvc.get().uri("/checkout").session(session).exchange();
    register(session, newUser());
    MvcTestResult created =
        validFields(mvc.post().uri("/account/new"), "0100-001-0001")
            .session(session)
            .with(csrf())
            .exchange();

    assertThat(created.getResponse().getRedirectedUrl()).contains("/checkout");
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private static String newUser() {
    return "user" + COUNTER.incrementAndGet();
  }

  /** A customer of its own for a test that changes it, so the seed customers stay untouched. */
  private String existingCustomer() {
    String username = newUser();
    customers.create(username, CustomerFixtures.details());
    return username;
  }

  private MvcTestResult register(MockHttpSession session, String username) {
    return mvc.post()
        .uri("/register")
        .param("j_username", username)
        .param("j_password", "secret")
        .param("j_password_2", "secret")
        .session(session)
        .with(csrf())
        .exchange();
  }

  private MvcTestResult validForm(MockMvcRequestBuilder request, String username) {
    return validForm(request, username, "0100-001-0001");
  }

  private MvcTestResult validForm(
      MockMvcRequestBuilder request, String username, String cardNumber) {
    return validFields(request, cardNumber).with(user(username)).with(csrf()).exchange();
  }

  private static MockMvcRequestBuilder validFields(
      MockMvcRequestBuilder request, String cardNumber) {
    return request
        .param("givenName", "Duke")
        .param("familyName", "BluesPrints")
        .param("address1", "1234 Moon Way")
        .param("address2", "")
        .param("city", "Sunville")
        .param("state", "California")
        .param("postalCode", "10001")
        .param("country", "USA")
        .param("telephone", "408-555-5555")
        .param("email", "duke@example.com")
        .param("cardNumber", cardNumber)
        .param("cardType", "Duke Express")
        .param("expiryMonth", "10")
        .param("expiryYear", "2031")
        .param("favoriteCategory", "DOGS")
        .param("myList", "on");
  }
}
