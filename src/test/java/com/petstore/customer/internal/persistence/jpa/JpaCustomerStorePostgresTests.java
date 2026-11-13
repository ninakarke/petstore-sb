package com.petstore.customer.internal.persistence.jpa;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The customer store contract on a real PostgreSQL server. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfiguration.class)
class JpaCustomerStorePostgresTests extends JpaCustomerStoreContractTests {}
