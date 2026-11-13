package com.petstore.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Profile;

/**
 * Marks a component that belongs to the MongoDB run mode, which is switched on by the {@code mongo}
 * profile. See {@link RelationalStore}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Profile(DocumentStore.PROFILE)
public @interface DocumentStore {

  /** The profile that selects MongoDB as the data store. */
  String PROFILE = "mongo";
}
