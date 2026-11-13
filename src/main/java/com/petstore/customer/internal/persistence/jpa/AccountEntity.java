package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code account} table. */
@Entity
@Table(name = "account")
class AccountEntity {

  @Id
  @Column(name = "userid")
  String userId;

  @Column(name = "status")
  String status;

  protected AccountEntity() {}

  AccountEntity(String userId) {
    this.userId = userId;
  }
}
