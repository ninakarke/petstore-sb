package com.petstore.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * Settings for loading the reference data a new, empty store starts with.
 *
 * @param enabled whether a module loads its part of the seed data when its store is empty
 * @param location the seed document; by default the data file of the legacy application
 */
@ConfigurationProperties("petstore.seed")
public record SeedProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("classpath:seed/Populate-UTF8.xml") Resource location) {}
