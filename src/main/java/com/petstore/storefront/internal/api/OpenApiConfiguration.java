package com.petstore.storefront.internal.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the JSON API. The description is generated from the controllers and served at {@code
 * /v3/api-docs}; an interactive page is at {@code /swagger-ui.html}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

  @Bean
  OpenAPI petstoreApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Pet Store API")
                .version("v1")
                .description(
                    "JSON interface of the Pet Store storefront. It exposes the same services as"
                        + " the web pages."));
  }
}
