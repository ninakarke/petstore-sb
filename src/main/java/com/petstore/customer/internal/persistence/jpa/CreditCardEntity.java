package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code credit_card} table. */
@Entity
@Table(name = "credit_card")
class CreditCardEntity {

  @Id
  @Column(name = "userid")
  String userId;

  @Column(name = "card_number")
  String cardNumber;

  @Column(name = "card_type")
  String cardType;

  @Column(name = "expiry_date")
  String expiryDate;

  protected CreditCardEntity() {}

  CreditCardEntity(String userId) {
    this.userId = userId;
  }
}
