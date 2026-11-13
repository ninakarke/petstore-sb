package com.petstore.storefront.internal.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The home page.
 *
 * <p>Legacy equivalent: the {@code main} screen ({@code main.screen}), assembled by the template
 * servlet from {@code screendefinitions_en_US.xml} and {@code main.jsp}. That page was one picture
 * with clickable areas, one per category. The picture may not be redistributed, so the page shows
 * it only when it is present in the local picture folder, and a tile per category otherwise.
 */
@Controller
class HomeController {

  private static final String SPLASH = "splash.gif";

  private final Pictures pictures;

  HomeController(Pictures pictures) {
    this.pictures = pictures;
  }

  @GetMapping("/")
  String home(Model model) {
    model.addAttribute("splashAvailable", pictures.isLocal(SPLASH));
    model.addAttribute("showAdviceBanner", true);
    return "home";
  }
}
