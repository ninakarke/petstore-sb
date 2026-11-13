package com.petstore.migration;

import org.springframework.test.context.ActiveProfiles;

/** The copy job with the embedded H2 database as the source. */
@ActiveProfiles("migrate-to-mongo")
class CopyFromH2ToMongoTests extends CopyToMongoTests {}
