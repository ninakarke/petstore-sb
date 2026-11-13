package com.petstore.identity.internal.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A sign-in user, one document in {@code users}. The user name is the document's key, so it is
 * unique and compared exactly, as the primary key of the relational table is.
 *
 * <p>Credentials are kept apart from the customer's details on purpose: they belong to the identity
 * module, and no page that shows a customer ever loads a password hash.
 */
@Document("users")
record UserDocument(@Id String username, String passwordHash) {}
