package com.petstore.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Profile;

/**
 * Marks a component that belongs to the relational run modes (embedded H2 and PostgreSQL).
 *
 * <p>Both database stacks are part of the application. Exactly one is switched on when it starts:
 * the relational one unless the {@code mongo} profile is active. Every persistence adapter carries
 * this annotation or {@link DocumentStore}, so each port always has exactly one implementation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Profile("!" + DocumentStore.PROFILE)
public @interface RelationalStore {}
