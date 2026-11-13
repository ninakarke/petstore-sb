package com.petstore.order.internal.persistence.mongo;

import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * An order, one document in {@code orders}: its lines, both addresses, the payment card and the
 * state of its hand-off to order processing are all inside it.
 *
 * <p>This is what makes the order flow safe without a transaction. Storing an order and recording
 * that it still has to be sent is one insert of one document, so the two can never disagree. A
 * separate "outbox" collection would need a transaction across two collections, and with it a
 * replica set.
 *
 * <p>Addresses, card and lines are copies of what applied when the order was placed, not
 * references: a later change to the customer or the catalog must not rewrite an order. The
 * relational schema keeps the same copies, as columns.
 */
@Document("orders")
record OrderDocument(
    @Id String orderId,
    String userId,
    String email,
    Instant placedAt,
    ContactPart shipTo,
    ContactPart billTo,
    CardPart creditCard,
    List<LinePart> lines,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal totalPrice,
    HandoffPart handoff,
    String idempotencyKey) {

  record ContactPart(
      String givenName, String familyName, String email, String telephone, AddressPart address) {

    static ContactPart of(ContactInfo contact) {
      Address address = contact.address();
      return new ContactPart(
          contact.givenName(),
          contact.familyName(),
          contact.email(),
          contact.telephone(),
          new AddressPart(
              address.streetName1(),
              address.streetName2(),
              address.city(),
              address.state(),
              address.zipCode(),
              address.country()));
    }

    ContactInfo toContactInfo() {
      return new ContactInfo(
          givenName,
          familyName,
          new Address(
              address.street1(),
              address.street2(),
              address.city(),
              address.state(),
              address.postalCode(),
              address.country()),
          email,
          telephone);
    }
  }

  record AddressPart(
      String street1,
      String street2,
      String city,
      String state,
      String postalCode,
      String country) {}

  record CardPart(String number, String type, String expiry) {}

  record LinePart(
      int lineNumber,
      String itemId,
      String productId,
      String categoryId,
      int quantity,
      @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPrice) {}

  record HandoffPart(HandoffStatus status, int attempts, Instant lastAttemptAt) {

    static HandoffPart of(Handoff handoff) {
      return new HandoffPart(handoff.status(), handoff.attempts(), handoff.lastAttemptAt());
    }
  }

  static OrderDocument of(Order order) {
    CreditCard card = order.creditCard();
    return new OrderDocument(
        order.orderId(),
        order.userId(),
        order.email(),
        order.placedAt(),
        ContactPart.of(order.shipTo()),
        ContactPart.of(order.billTo()),
        new CardPart(card.cardNumber(), card.cardType(), card.expiryDate()),
        order.lines().stream()
            .map(line ->
                new LinePart(
                    line.lineNumber(),
                    line.itemId(),
                    line.productId(),
                    line.categoryId(),
                    line.quantity(),
                    line.unitPrice()))
            .toList(),
        order.totalPrice(),
        HandoffPart.of(order.handoff()),
        order.idempotencyKey());
  }

  Order toOrder() {
    return new Order(
        orderId,
        userId,
        email,
        placedAt,
        shipTo.toContactInfo(),
        billTo.toContactInfo(),
        new CreditCard(creditCard.number(), creditCard.type(), creditCard.expiry()),
        lines.stream()
            .map(line ->
                new OrderLine(
                    line.categoryId(),
                    line.productId(),
                    line.itemId(),
                    line.lineNumber(),
                    line.quantity(),
                    line.unitPrice()))
            .toList(),
        totalPrice,
        new Handoff(handoff.status(), handoff.attempts(), handoff.lastAttemptAt()),
        idempotencyKey);
  }
}
