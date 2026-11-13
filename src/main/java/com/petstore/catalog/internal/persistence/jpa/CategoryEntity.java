package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code category} table. */
@Entity
@Table(name = "category")
class CategoryEntity {

  @Id
  @Column(name = "catid")
  private String id;

  protected CategoryEntity() {}

  CategoryEntity(String id) {
    this.id = id;
  }
}
