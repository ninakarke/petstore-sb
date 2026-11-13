package com.petstore.catalog.internal;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The legacy SQL comparison on a real PostgreSQL server. Skipped when Docker is not available. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("postgres")
@Import(PostgresContainerConfiguration.class)
class CatalogLegacySqlDifferentialPostgresTests extends CatalogLegacySqlDifferentialTests {}
