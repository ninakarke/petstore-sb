package com.petstore.catalog.internal;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The relational adapter on PostgreSQL, compared with the reference store. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("postgres")
@Import(PostgresContainerConfiguration.class)
class CatalogEquivalencePostgresTests extends CatalogEquivalenceTests {}
