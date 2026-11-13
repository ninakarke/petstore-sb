package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code product} table. */
@Entity
@Table(name = "product")
class ProductEntity {

  @Id
  @Column(name = "productid")
  private String id;

  @Column(name = "catid")
  private String categoryId;

  protected ProductEntity() {}

  ProductEntity(String id, String categoryId) {
    this.id = id;
    this.categoryId = categoryId;
  }
}
