package com.petstore.catalog.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;

/** Row of the {@code item_details} table: text and prices of an item in one locale. */
@Entity
@Table(name = "item_details")
class ItemDetailsEntity {

  @EmbeddedId private ItemDetailsId id;

  @Column(name = "listprice", precision = 10, scale = 2)
  private BigDecimal listPrice;

  @Column(name = "unitcost", precision = 10, scale = 2)
  private BigDecimal unitCost;

  @Column(name = "image")
  private String image;

  @Column(name = "descn")
  private String description;

  @Column(name = "attr1")
  private String attr1;

  @Column(name = "attr2")
  private String attr2;

  @Column(name = "attr3")
  private String attr3;

  @Column(name = "attr4")
  private String attr4;

  @Column(name = "attr5")
  private String attr5;

  protected ItemDetailsEntity() {}

  ItemDetailsEntity(
      ItemDetailsId id,
      BigDecimal listPrice,
      BigDecimal unitCost,
      String image,
      String description,
      List<String> attributes) {
    this.id = id;
    this.listPrice = listPrice;
    this.unitCost = unitCost;
    this.image = image;
    this.description = description;
    this.attr1 = attribute(attributes, 0);
    this.attr2 = attribute(attributes, 1);
    this.attr3 = attribute(attributes, 2);
    this.attr4 = attribute(attributes, 3);
    this.attr5 = attribute(attributes, 4);
  }

  private static String attribute(List<String> attributes, int index) {
    return index < attributes.size() ? attributes.get(index) : null;
  }
}
