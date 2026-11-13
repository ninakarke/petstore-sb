package com.petstore.customer.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code profile} table. */
@Entity
@Table(name = "profile")
class ProfileEntity {

  @Id
  @Column(name = "userid")
  String userId;

  @Column(name = "preferred_language")
  String preferredLanguage;

  @Column(name = "favorite_category")
  String favoriteCategory;

  @Column(name = "my_list_preference")
  boolean myListPreference;

  @Column(name = "banner_preference")
  boolean bannerPreference;

  protected ProfileEntity() {}

  ProfileEntity(String userId) {
    this.userId = userId;
  }
}
