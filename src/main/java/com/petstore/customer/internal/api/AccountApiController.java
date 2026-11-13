package com.petstore.customer.internal.api;

import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.CustomerDetails;
import com.petstore.customer.CustomerField;
import com.petstore.customer.CustomerService;
import com.petstore.customer.InvalidCustomerException;
import com.petstore.customer.Profile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The signed-in customer's own account as JSON.
 *
 * <p>The legacy application had no such interface. It is a second, thin adapter over {@link
 * CustomerService}, with the same rules as the account pages: the same required fields, the card
 * number only ever shown masked, and an omitted card number keeping the card on file. A caller can
 * only reach the account of the user their token belongs to.
 */
@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "The caller's own customer account. Requires a bearer token.")
@SecurityRequirement(name = "bearer-jwt")
class AccountApiController {

  private final CustomerService customers;

  AccountApiController(CustomerService customers) {
    this.customers = customers;
  }

  @GetMapping
  @Operation(summary = "Get the caller's account")
  AccountResponse get(Principal caller) {
    return customers
        .find(caller.getName())
        .map(AccountResponse::of)
        .orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.NOT_FOUND, "This user has not entered account details yet"));
  }

  @PutMapping
  @Operation(
      summary = "Create or replace the caller's account details",
      description =
          "The card number may be omitted when the account already has a card; the card on file"
              + " is then kept.")
  AccountResponse put(@RequestBody AccountRequest request, Principal caller) {
    if (request.contact() == null
        || request.contact().address() == null
        || request.card() == null
        || request.profile() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "contact (with address), card and profile are required");
    }
    Optional<Customer> existing = customers.find(caller.getName());
    String storedCardNumber =
        existing.map(customer -> customer.account().creditCard().cardNumber()).orElse(null);
    String storedLanguage =
        existing.map(customer -> customer.profile().preferredLanguage()).orElse(null);
    CustomerDetails details = request.toDetails(storedCardNumber, storedLanguage);
    Customer saved =
        existing.isPresent()
            ? customers.update(caller.getName(), details)
            : customers.create(caller.getName(), details);
    return AccountResponse.of(saved);
  }

  /** Missing or unusable fields are named in the problem detail, under {@code fields}. */
  @ExceptionHandler(InvalidCustomerException.class)
  ProblemDetail invalid(InvalidCustomerException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Some account details are missing or not usable");
    problem.setProperty(
        "fields", exception.fields().stream().map(CustomerField::name).sorted().toList());
    return problem;
  }

  // ---- request -----------------------------------------------------------------------------

  record AccountRequest(ContactInfo contact, CardRequest card, ProfileRequest profile) {

    CustomerDetails toDetails(String storedCardNumber, String storedLanguage) {
      boolean keepStoredCard =
          (card.cardNumber() == null || card.cardNumber().isBlank()) && storedCardNumber != null;
      return new CustomerDetails(
          contact,
          CreditCard.of(
              keepStoredCard ? storedCardNumber : card.cardNumber(),
              card.cardType(),
              text(card.expiryMonth()),
              text(card.expiryYear())),
          new Profile(
              storedLanguage,
              profile.favoriteCategory(),
              Boolean.TRUE.equals(profile.myList()),
              Boolean.TRUE.equals(profile.banners())));
    }

    private static String text(String value) {
      return value == null ? "" : value;
    }
  }

  record CardRequest(String cardNumber, String cardType, String expiryMonth, String expiryYear) {}

  record ProfileRequest(String favoriteCategory, Boolean myList, Boolean banners) {}

  // ---- response ----------------------------------------------------------------------------

  /**
   * @param card the payment card, with the number masked to its last four characters
   */
  record AccountResponse(
      String userId,
      String status,
      ContactInfo contact,
      CardResponse card,
      ProfileResponse profile) {

    static AccountResponse of(Customer customer) {
      CreditCard card = customer.account().creditCard();
      Profile profile = customer.profile();
      return new AccountResponse(
          customer.userId(),
          customer.account().status(),
          customer.account().contactInfo(),
          new CardResponse(
              card.cardType(), card.maskedNumber(), card.expiryMonth(), card.expiryYear()),
          new ProfileResponse(
              profile.favoriteCategory(), profile.myListPreference(), profile.bannerPreference()));
    }
  }

  record CardResponse(
      String cardType, String maskedNumber, String expiryMonth, String expiryYear) {}

  record ProfileResponse(String favoriteCategory, boolean myList, boolean banners) {}
}
