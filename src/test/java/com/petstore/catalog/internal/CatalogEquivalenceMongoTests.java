package com.petstore.catalog.internal;

import com.petstore.testsupport.MongoContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The MongoDB adapter, compared with the reference store. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("mongo")
@Import(MongoContainerConfiguration.class)
class CatalogEquivalenceMongoTests extends CatalogEquivalenceTests {}
