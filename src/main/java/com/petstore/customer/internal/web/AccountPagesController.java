package com.petstore.customer.internal.web;

import com.petstore.customer.Customer;
import com.petstore.customer.CustomerService;
import com.petstore.customer.InvalidCustomerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The account pages: entering the details of a new customer, viewing them and changing them.
 *
 * <p>Legacy equivalents: the screens {@code create_customer}, {@code customer} and {@code
 * update_customer}, and the actions {@code createcustomer.do} and {@code customer.do}, handled by
 * {@code CustomerHTMLAction}. All of these pages require a signed-in user.
 */
@Controller
class AccountPagesController {

  private static final String FORM_VIEW = "customer/account-form";

  private final CustomerService customers;
  private final RequestCache requestCache = new HttpSessionRequestCache();

  AccountPagesController(CustomerService customers) {
    this.customers = customers;
  }

  /** Legacy: {@code customer.do} / {@code customer.screen}. */
  @GetMapping("/account")
  String show(Principal user, Model model) {
    Optional<Customer> customer = customers.find(user.getName());
    if (customer.isEmpty()) {
      return "redirect:/account/new";
    }
    model.addAttribute("customer", customer.get());
    return "customer/account";
  }

  /** Legacy: {@code create_customer.screen}. */
  @GetMapping("/account/new")
  String createForm(Principal user, Model model) {
    if (customers.exists(user.getName())) {
      return "redirect:/account";
    }
    return form(AccountForm.empty(), true, null, null, model);
  }

  /**
   * Legacy: {@code createcustomer.do}. As there, the visitor is then sent on to the page they were
   * originally trying to reach (for example the order form), if there was one.
   */
  @PostMapping("/account/new")
  String create(
      @RequestParam Map<String, String> parameters,
      Principal user,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (customers.exists(user.getName())) {
      return "redirect:/account";
    }
    AccountForm form = AccountForm.submitted(parameters);
    try {
      customers.create(user.getName(), form.toDetails(null, null));
    } catch (InvalidCustomerException invalid) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      return form(form, true, null, invalid, model);
    }
    SavedRequest interrupted = requestCache.getRequest(request, response);
    if (interrupted != null) {
      requestCache.removeRequest(request, response);
      return "redirect:" + interrupted.getRedirectUrl();
    }
    return "redirect:/account";
  }

  /** Legacy: {@code update_customer.screen}. */
  @GetMapping("/account/edit")
  String editForm(Principal user, Model model) {
    Optional<Customer> customer = customers.find(user.getName());
    if (customer.isEmpty()) {
      return "redirect:/account/new";
    }
    return form(AccountForm.of(customer.get()), false, customer.get(), null, model);
  }

  /** Legacy: {@code customer.do?action=update}. */
  @PostMapping("/account")
  String update(
      @RequestParam Map<String, String> parameters,
      Principal user,
      HttpServletResponse response,
      Model model) {
    Optional<Customer> existing = customers.find(user.getName());
    if (existing.isEmpty()) {
      return "redirect:/account/new";
    }
    AccountForm form = AccountForm.submitted(parameters);
    try {
      customers.update(
          user.getName(),
          form.toDetails(
              existing.get().account().creditCard().cardNumber(),
              existing.get().profile().preferredLanguage()));
    } catch (InvalidCustomerException invalid) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      return form(form, false, existing.get(), invalid, model);
    }
    return "redirect:/account";
  }

  private String form(
      AccountForm form,
      boolean creating,
      Customer existing,
      InvalidCustomerException invalid,
      Model model) {
    model.addAttribute("form", form);
    model.addAttribute("creating", creating);
    model.addAttribute("options", AccountFormOptions.forForm(form, Year.now()));
    model.addAttribute("invalidFields", invalid == null ? Set.of() : invalid.fields());
    model.addAttribute(
        "storedCardNumber", existing == null ? "" : existing.account().creditCard().maskedNumber());
    return FORM_VIEW;
  }
}
