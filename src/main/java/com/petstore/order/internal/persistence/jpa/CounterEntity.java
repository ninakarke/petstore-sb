package com.petstore.order.internal.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the {@code counter} table. Legacy: the {@code Counter} entity bean. */
@Entity
@Table(name = "counter")
class CounterEntity {

  @Id
  @Column(name = "name")
  String name;

  @Column(name = "counter")
  int value;

  protected CounterEntity() {}
}
