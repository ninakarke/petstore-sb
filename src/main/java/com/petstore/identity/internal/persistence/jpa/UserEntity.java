package com.petstore.identity.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code signon_user} table. */
@Entity
@Table(name = "signon_user")
class UserEntity {

  @Id
  @Column(name = "username")
  private String username;

  @Column(name = "password")
  private String passwordHash;

  protected UserEntity() {}

  UserEntity(String username, String passwordHash) {
    this.username = username;
    this.passwordHash = passwordHash;
  }

  String username() {
    return username;
  }

  String passwordHash() {
    return passwordHash;
  }
}
