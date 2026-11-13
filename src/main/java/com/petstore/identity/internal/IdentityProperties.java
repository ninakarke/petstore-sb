package com.petstore.identity.internal;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security settings.
 *
 * @param protectedPaths the pages that require a signed-in customer. Legacy equivalent: the {@code
 *     <constraint>} entries of {@code WEB-INF/signon-config.xml}.
 */
@ConfigurationProperties("petstore.security")
public record IdentityProperties(
    @DefaultValue({"/account/**", "/checkout/**", "/orders/**"}) List<String> protectedPaths) {}
