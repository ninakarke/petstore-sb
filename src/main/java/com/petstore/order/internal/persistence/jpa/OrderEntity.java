package com.petstore.order.internal.persistence.jpa;

import com.petstore.customer.CreditCard;
import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of the {@code purchase_order} table with its {@code order_line} rows: one order, stored and
 * loaded as a whole.
 */
@Entity
@Table(name = "purchase_order")
class OrderEntity {

  @Id
  @Column(name = "order_id")
  String orderId;

  @Column(name = "user_id")
  String userId;

  @Column(name = "email")
  String email;

  @Column(name = "placed_at")
  Instant placedAt;

  @Column(name = "total_price", precision = 12, scale = 2)
  BigDecimal totalPrice;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "givenName", column = @Column(name = "ship_given_name")),
    @AttributeOverride(name = "familyName", column = @Column(name = "ship_family_name")),
    @AttributeOverride(name = "streetName1", column = @Column(name = "ship_street_name1")),
    @AttributeOverride(name = "streetName2", column = @Column(name = "ship_street_name2")),
    @AttributeOverride(name = "city", column = @Column(name = "ship_city")),
    @AttributeOverride(name = "state", column = @Column(name = "ship_state")),
    @AttributeOverride(name = "zipCode", column = @Column(name = "ship_zip_code")),
    @AttributeOverride(name = "country", column = @Column(name = "ship_country")),
    @AttributeOverride(name = "email", column = @Column(name = "ship_email")),
    @AttributeOverride(name = "telephone", column = @Column(name = "ship_telephone"))
  })
  ContactEmbeddable shipTo;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "givenName", column = @Column(name = "bill_given_name")),
    @AttributeOverride(name = "familyName", column = @Column(name = "bill_family_name")),
    @AttributeOverride(name = "streetName1", column = @Column(name = "bill_street_name1")),
    @AttributeOverride(name = "streetName2", column = @Column(name = "bill_street_name2")),
    @AttributeOverride(name = "city", column = @Column(name = "bill_city")),
    @AttributeOverride(name = "state", column = @Column(name = "bill_state")),
    @AttributeOverride(name = "zipCode", column = @Column(name = "bill_zip_code")),
    @AttributeOverride(name = "country", column = @Column(name = "bill_country")),
    @AttributeOverride(name = "email", column = @Column(name = "bill_email")),
    @AttributeOverride(name = "telephone", column = @Column(name = "bill_telephone"))
  })
  ContactEmbeddable billTo;

  @Column(name = "card_number")
  String cardNumber;

  @Column(name = "card_type")
  String cardType;

  @Column(name = "card_expiry_date")
  String cardExpiryDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "handoff_status")
  HandoffStatus handoffStatus;

  @Column(name = "handoff_attempts")
  int handoffAttempts;

  @Column(name = "handoff_last_attempt_at")
  Instant handoffLastAttemptAt;

  @Column(name = "idempotency_key")
  String idempotencyKey;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
  @OrderBy("lineNumber")
  List<OrderLineEmbeddable> lines = new ArrayList<>();

  protected OrderEntity() {}

  OrderEntity(Order order) {
    this.orderId = order.orderId();
    this.userId = order.userId();
    this.email = order.email();
    this.placedAt = order.placedAt();
    this.totalPrice = order.totalPrice();
    this.shipTo = new ContactEmbeddable(order.shipTo());
    this.billTo = new ContactEmbeddable(order.billTo());
    this.cardNumber = order.creditCard().cardNumber();
    this.cardType = order.creditCard().cardType();
    this.cardExpiryDate = order.creditCard().expiryDate();
    this.idempotencyKey = order.idempotencyKey();
    order.lines().forEach(line -> lines.add(new OrderLineEmbeddable(line)));
    setHandoff(order.handoff());
  }

  void setHandoff(Handoff handoff) {
    this.handoffStatus = handoff.status();
    this.handoffAttempts = handoff.attempts();
    this.handoffLastAttemptAt = handoff.lastAttemptAt();
  }

  Order toOrder() {
    return new Order(
        orderId,
        userId,
        email,
        placedAt,
        shipTo.toContactInfo(),
        billTo.toContactInfo(),
        new CreditCard(cardNumber, cardType, cardExpiryDate),
        lines.stream().map(OrderLineEmbeddable::toOrderLine).toList(),
        totalPrice,
        new Handoff(handoffStatus, handoffAttempts, handoffLastAttemptAt),
        idempotencyKey);
  }
}
