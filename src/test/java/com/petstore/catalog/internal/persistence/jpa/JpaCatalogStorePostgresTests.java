package com.petstore.catalog.internal.persistence.jpa;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The catalog store contract on a real PostgreSQL server. Same tests as on H2; passing both is what
 * shows the embedded database is a faithful stand-in. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfiguration.class)
class JpaCatalogStorePostgresTests extends JpaCatalogStoreContractTests {}
