package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code address} table. */
@Entity
@Table(name = "address")
class AddressEntity {

  @Id
  @Column(name = "userid")
  String userId;

  @Column(name = "street_name1")
  String streetName1;

  @Column(name = "street_name2")
  String streetName2;

  @Column(name = "city")
  String city;

  @Column(name = "state")
  String state;

  @Column(name = "zip_code")
  String zipCode;

  @Column(name = "country")
  String country;

  protected AddressEntity() {}

  AddressEntity(String userId) {
    this.userId = userId;
  }
}
