package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code contact_info} table. */
@Entity
@Table(name = "contact_info")
class ContactInfoEntity {

  @Id
  @Column(name = "userid")
  String userId;

  @Column(name = "given_name")
  String givenName;

  @Column(name = "family_name")
  String familyName;

  @Column(name = "telephone")
  String telephone;

  @Column(name = "email")
  String email;

  protected ContactInfoEntity() {}

  ContactInfoEntity(String userId) {
    this.userId = userId;
  }
}
