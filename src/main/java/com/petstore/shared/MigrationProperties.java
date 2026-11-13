package com.petstore.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the job that copies data to MongoDB.
 *
 * @param batchSize how many records are read from the source at a time
 * @param exitWhenDone whether the process ends when the copy is finished, with exit code 0 if the
 *     two stores match and 1 if they do not
 */
@ConfigurationProperties("petstore.migration")
public record MigrationProperties(
    @DefaultValue("500") int batchSize, @DefaultValue("true") boolean exitWhenDone) {}
