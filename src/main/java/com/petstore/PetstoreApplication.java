package com.petstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Pet Store storefront. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PetstoreApplication {

  public static void main(String[] args) {
    SpringApplication.run(PetstoreApplication.class, args);
  }
}
