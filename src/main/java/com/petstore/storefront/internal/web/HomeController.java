package com.petstore.storefront.internal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The home page.
 *
 * <p>Legacy equivalent: the {@code main} screen ({@code main.screen}), assembled by the template
 * servlet from {@code screendefinitions_en_US.xml} and {@code main.jsp}.
 */
@Controller
class HomeController {

  @GetMapping("/")
  String home() {
    return "home";
  }
}
