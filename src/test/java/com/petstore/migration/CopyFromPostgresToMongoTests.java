package com.petstore.migration;

import com.petstore.testsupport.PostgresContainerConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** The copy job with a real PostgreSQL server as the source: the realistic case. */
@ActiveProfiles({"postgres", "migrate-to-mongo"})
@Import(PostgresContainerConfiguration.class)
class CopyFromPostgresToMongoTests extends CopyToMongoTests {}
