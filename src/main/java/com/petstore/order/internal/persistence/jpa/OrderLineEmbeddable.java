package com.petstore.order.internal.persistence.jpa;

import com.petstore.order.OrderLine;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Row of the {@code order_line} table. */
@Embeddable
class OrderLineEmbeddable {

  @Column(name = "line_number")
  int lineNumber;

  @Column(name = "category_id")
  String categoryId;

  @Column(name = "product_id")
  String productId;

  @Column(name = "item_id")
  String itemId;

  @Column(name = "quantity")
  int quantity;

  @Column(name = "unit_price", precision = 10, scale = 2)
  BigDecimal unitPrice;

  protected OrderLineEmbeddable() {}

  OrderLineEmbeddable(OrderLine line) {
    this.lineNumber = line.lineNumber();
    this.categoryId = line.categoryId();
    this.productId = line.productId();
    this.itemId = line.itemId();
    this.quantity = line.quantity();
    this.unitPrice = line.unitPrice();
  }

  OrderLine toOrderLine() {
    return new OrderLine(categoryId, productId, itemId, lineNumber, quantity, unitPrice);
  }
}
