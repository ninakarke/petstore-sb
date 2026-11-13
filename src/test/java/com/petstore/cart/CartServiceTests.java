package com.petstore.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.cart.internal.SessionCart;
import com.petstore.catalog.CatalogService;
import com.petstore.catalog.Item;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rules of the shopping cart.
 *
 * <p>Legacy sources: {@code components/cart/.../ejb/ShoppingCartLocalEJB.java}, {@code
 * components/cart/.../model/CartItem.java} and {@code
 * apps/petstore/.../controller/ejb/actions/CartEJBAction.java}.
 */
class CartServiceTests {

  private final CatalogService catalog = mock(CatalogService.class);
  private final CartService cart = new CartService(new SessionCart(), catalog);

  @BeforeEach
  void catalogKnowsThreeItems() {
    when(catalog.item(anyString())).thenReturn(Optional.empty());
    when(catalog.item("EST-1")).thenReturn(Optional.of(item("EST-1", "Large", "16.50")));
    when(catalog.item("EST-6")).thenReturn(Optional.of(item("EST-6", "Male Adult", "18.50")));
    when(catalog.item("EST-21")).thenReturn(Optional.of(item("EST-21", "Adult Male", "5.29")));
  }

  @Test
  @DisplayName("legacy: a new cart is empty")
  void newCartIsEmpty() {
    CartView view = cart.view();

    assertThat(view.isEmpty()).isTrue();
    assertThat(view.count()).isZero();
    assertThat(view.subTotal()).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("legacy: adding an item puts it in the cart with quantity one")
  void addingAnItemPutsOneInTheCart() {
    cart.add("EST-1");

    assertThat(cart.view().lines())
        .singleElement()
        .satisfies(line -> {
          assertThat(line.itemId()).isEqualTo("EST-1");
          assertThat(line.quantity()).isEqualTo(1);
          assertThat(line.displayName()).isEqualTo("Large Angelfish");
          assertThat(line.productId()).isEqualTo("FI-SW-01");
          assertThat(line.categoryId()).isEqualTo("FISH");
        });
  }

  @Test
  @DisplayName("legacy: adding an item that is already in the cart sets its quantity back to one")
  void addingAgainDoesNotIncreaseTheQuantity() {
    cart.add("EST-1");
    cart.updateQuantities(Map.of("EST-1", 5));

    cart.add("EST-1");

    assertThat(cart.view().lines()).extracting(CartLine::quantity).containsExactly(1);
  }

  @Test
  @DisplayName("legacy: the price of a line is the item's list price, not the customer's price")
  void linePriceIsTheListPrice() {
    cart.add("EST-1");

    CartLine line = cart.view().lines().get(0);
    assertThat(line.unitPrice()).isEqualByComparingTo("16.50");
  }

  @Test
  @DisplayName("legacy: the count is the number of different items, not the sum of quantities")
  void countIsTheNumberOfDifferentItems() {
    cart.add("EST-1");
    cart.add("EST-6");
    cart.updateQuantities(Map.of("EST-1", 7));

    assertThat(cart.view().count()).isEqualTo(2);
  }

  @Test
  @DisplayName("legacy: updating sets the quantity")
  void updatingSetsTheQuantity() {
    cart.add("EST-1");

    cart.updateQuantities(Map.of("EST-1", 3));

    assertThat(cart.view().lines()).extracting(CartLine::quantity).containsExactly(3);
  }

  @Test
  @DisplayName("legacy: a quantity of zero or less removes the line")
  void zeroOrNegativeQuantityRemovesTheLine() {
    cart.add("EST-1");
    cart.add("EST-6");

    cart.updateQuantities(Map.of("EST-1", 0, "EST-6", -2));

    assertThat(cart.view().isEmpty()).isTrue();
  }

  @Test
  @DisplayName("legacy: removing takes the line out; removing what is not there does nothing")
  void removingTakesTheLineOut() {
    cart.add("EST-1");
    cart.add("EST-6");

    cart.remove("EST-1");
    cart.remove("EST-404");

    assertThat(cart.view().lines()).extracting(CartLine::itemId).containsExactly("EST-6");
  }

  @Test
  @DisplayName("legacy: the total is the sum of list price times quantity")
  void subTotalIsTheSumOfTheLineTotals() {
    cart.add("EST-1");
    cart.add("EST-6");
    cart.updateQuantities(Map.of("EST-1", 3));

    CartView view = cart.view();

    assertThat(view.lines().get(0).total()).isEqualByComparingTo("49.50");
    assertThat(view.subTotal()).isEqualByComparingTo("68.00");
  }

  @Test
  @DisplayName("fixed: totals are exact decimal arithmetic (legacy summed doubles)")
  void totalsAreExact() {
    cart.add("EST-21");
    cart.updateQuantities(Map.of("EST-21", 3));

    // 3 x 5.29 is 15.870000000000001 in double arithmetic.
    assertThat(5.29 * 3).isNotEqualTo(15.87);
    assertThat(cart.view().subTotal()).isEqualTo(new BigDecimal("15.87"));
  }

  @Test
  @DisplayName("legacy: emptying removes every line")
  void emptyingRemovesEverything() {
    cart.add("EST-1");
    cart.add("EST-6");

    cart.empty();

    assertThat(cart.view().isEmpty()).isTrue();
  }

  @Test
  @DisplayName("fixed: lines keep the order in which they were added (legacy order was undefined)")
  void linesKeepTheOrderInWhichTheyWereAdded() {
    cart.add("EST-6");
    cart.add("EST-21");
    cart.add("EST-1");

    assertThat(cart.view().lines())
        .extracting(CartLine::itemId)
        .containsExactly("EST-6", "EST-21", "EST-1");
  }

  @Test
  @DisplayName("fixed: an item that is not in the catalog cannot be added (legacy accepted any id)")
  void unknownItemCannotBeAdded() {
    assertThatThrownBy(() -> cart.add("EST-404"))
        .isInstanceOf(UnknownItemException.class)
        .hasMessageContaining("EST-404");
    assertThat(cart.view().isEmpty()).isTrue();
  }

  @Test
  @DisplayName("fixed: updating cannot put new items in the cart (legacy update added them)")
  void updatingIgnoresItemsThatAreNotInTheCart() {
    cart.add("EST-1");
    Map<String, Integer> quantities = new LinkedHashMap<>();
    quantities.put("EST-6", 4);
    quantities.put("NOT-AN-ITEM", 9);

    cart.updateQuantities(quantities);

    assertThat(cart.view().lines()).extracting(CartLine::itemId).containsExactly("EST-1");
  }

  @Test
  @DisplayName("an item that has left the catalog is left out of the cart view")
  void itemThatLeftTheCatalogIsLeftOut() {
    cart.add("EST-1");
    cart.add("EST-6");
    when(catalog.item("EST-1")).thenReturn(Optional.empty());

    assertThat(cart.view().lines()).extracting(CartLine::itemId).containsExactly("EST-6");
  }

  private static Item item(String itemId, String attribute, String listPrice) {
    return new Item(
        "FISH",
        "FI-SW-01",
        "Angelfish",
        itemId,
        "fish3.gif",
        "A fish",
        List.of(attribute),
        new BigDecimal(listPrice),
        new BigDecimal("1.00"));
  }
}
