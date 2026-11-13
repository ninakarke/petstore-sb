package com.petstore.storefront.internal.web;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where pictures come from.
 *
 * @param localDirectory a folder that is looked in first for a picture file. It is meant for the
 *     original Java Pet Store images, which may not be redistributed and are therefore not part of
 *     this repository; the folder is ignored by git.
 */
@ConfigurationProperties("petstore.images")
public record ImageProperties(@DefaultValue("local/legacy-images") Path localDirectory) {}
