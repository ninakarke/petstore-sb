package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code customer} table. */
@Entity
@Table(name = "customer")
class CustomerEntity {

  @Id
  @Column(name = "userid")
  String userId;

  protected CustomerEntity() {}

  CustomerEntity(String userId) {
    this.userId = userId;
  }
}
