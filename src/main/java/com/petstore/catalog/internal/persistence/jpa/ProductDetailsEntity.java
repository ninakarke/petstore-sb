package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Row of the {@code product_details} table: the text of a product in one locale. */
@Entity
@Table(name = "product_details")
class ProductDetailsEntity {

  @EmbeddedId private ProductDetailsId id;

  @Column(name = "name")
  private String name;

  @Column(name = "image")
  private String image;

  @Column(name = "descn")
  private String description;

  protected ProductDetailsEntity() {}

  ProductDetailsEntity(ProductDetailsId id, String name, String image, String description) {
    this.id = id;
    this.name = name;
    this.image = image;
    this.description = description;
  }
}
