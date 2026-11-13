package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code item} table. */
@Entity
@Table(name = "item")
class ItemEntity {

  @Id
  @Column(name = "itemid")
  private String id;

  @Column(name = "productid")
  private String productId;

  protected ItemEntity() {}

  ItemEntity(String id, String productId) {
    this.id = id;
    this.productId = productId;
  }
}
