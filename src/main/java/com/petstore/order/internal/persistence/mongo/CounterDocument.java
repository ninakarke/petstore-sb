package com.petstore.order.internal.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A named counter, one document in {@code counters}. Legacy equivalent: the {@code Counter} entity
 * bean and its table, with the same two values.
 */
@Document("counters")
record CounterDocument(@Id String name, long value) {}
