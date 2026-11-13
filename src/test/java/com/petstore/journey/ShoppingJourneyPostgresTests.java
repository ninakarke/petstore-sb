package com.petstore.journey;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The same user journeys on a real PostgreSQL server. Skipped when Docker is not available. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("postgres")
@Import(PostgresContainerConfiguration.class)
class ShoppingJourneyPostgresTests extends ShoppingJourneyTests {}
