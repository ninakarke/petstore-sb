package com.petstore.testsupport;

import com.petstore.shared.internal.persistence.mongo.MongoStoreConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A test of one MongoDB adapter on a real MongoDB server: only the MongoDB part of the application
 * is started, with the application's own mapping configuration, against a server in a container.
 * The test class imports the adapter it tests. Skipped when Docker is not available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@DataMongoTest
@ActiveProfiles("mongo")
@Testcontainers(disabledWithoutDocker = true)
@Import({MongoStoreConfiguration.class, MongoContainerConfiguration.class})
public @interface MongoStoreTest {}
