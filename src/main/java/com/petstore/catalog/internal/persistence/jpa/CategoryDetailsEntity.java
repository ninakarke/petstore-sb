package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Row of the {@code category_details} table: the text of a category in one locale. */
@Entity
@Table(name = "category_details")
class CategoryDetailsEntity {

  @EmbeddedId private CategoryDetailsId id;

  @Column(name = "name")
  private String name;

  @Column(name = "image")
  private String image;

  @Column(name = "descn")
  private String description;

  protected CategoryDetailsEntity() {}

  CategoryDetailsEntity(CategoryDetailsId id, String name, String image, String description) {
    this.id = id;
    this.name = name;
    this.image = image;
    this.description = description;
  }
}
