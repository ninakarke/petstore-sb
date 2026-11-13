package com.petstore.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.PetstoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Guards the module structure of the application.
 *
 * <p>The legacy storefront was built from separate component jars, but nothing other than the build
 * order kept them apart. Here each component is a module, and the build fails when a module reaches
 * into another module's internals, uses a module it has not declared, or takes part in a dependency
 * cycle.
 */
class ModuleStructureTests {

  private final ApplicationModules modules = ApplicationModules.of(PetstoreApplication.class);

  @Test
  void modulesMirrorTheLegacyComponents() {
    assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
        .containsExactlyInAnyOrder(
            "shared", "catalog", "cart", "identity", "customer", "order", "storefront");
  }

  @Test
  void modulesRespectTheirBoundaries() {
    modules.verify();
  }

  /** Writes module diagrams and canvases to {@code target/spring-modulith-docs}. */
  @Test
  void moduleDocumentationIsGeneratedFromTheCode() {
    new Documenter(modules).writeDocumentation();
  }
}
