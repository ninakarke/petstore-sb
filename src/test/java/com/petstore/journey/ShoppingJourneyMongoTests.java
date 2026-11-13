package com.petstore.journey;

import com.petstore.testsupport.MongoContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The same user journeys with MongoDB as the data store. Not one line of the journeys, the pages,
 * the API or the services differs from the relational runs. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("mongo")
@Import(MongoContainerConfiguration.class)
class ShoppingJourneyMongoTests extends ShoppingJourneyTests {}
