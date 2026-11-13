package com.petstore.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture rules that hold inside every module.
 *
 * <p>Layout of a module:
 *
 * <pre>
 * com.petstore.&lt;module&gt;                        public API and domain model
 * com.petstore.&lt;module&gt;.internal               ports and implementation details
 * com.petstore.&lt;module&gt;.internal.persistence   one sub-package per data store
 * com.petstore.&lt;module&gt;.internal.web           server-rendered pages
 * com.petstore.&lt;module&gt;.internal.api           JSON endpoints
 * </pre>
 *
 * <p>Rules that match nothing yet are allowed to be empty: they are written before the code they
 * constrain, so the constraint is in place when the first class arrives.
 */
@AnalyzeClasses(packages = "com.petstore", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTests {

  @ArchTest
  static final ArchRule legacyEnterpriseApisAreNotUsed =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "javax.ejb..",
              "javax.servlet..",
              "javax.persistence..",
              "javax.jms..",
              "javax.naming..",
              "java.rmi..")
          .because(
              "the EJB, servlet, JNDI and RMI programming model of the legacy application is"
                  + " replaced by dependency injection and jakarta.* APIs");

  @ArchTest
  static final ArchRule persistenceTechnologyStaysInTheAdapters =
      noClasses()
          .that()
          .resideOutsideOfPackage("..internal.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.persistence..",
              "org.springframework.data.jpa..",
              "org.springframework.data.mongodb..",
              "com.mongodb..",
              "org.bson..")
          .because(
              "business logic talks to a port; only an adapter may know which database is behind"
                  + " it, so the database can be changed without touching the logic")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule relationalAdapterDoesNotKnowTheDocumentAdapter =
      noClasses()
          .that()
          .resideInAPackage("..internal.persistence.jpa..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..internal.persistence.mongo..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule documentAdapterDoesNotKnowTheRelationalAdapter =
      noClasses()
          .that()
          .resideInAPackage("..internal.persistence.mongo..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..internal.persistence.jpa..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule pagesAndEndpointsDoNotBypassTheServices =
      noClasses()
          .that()
          .resideInAnyPackage("..internal.web..", "..internal.api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..internal.persistence..")
          .because(
              "the legacy pages read the database directly through a DAO; here every read goes"
                  + " through a service")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule persistenceAdaptersDoNotDependOnTheWebLayer =
      noClasses()
          .that()
          .resideInAPackage("..internal.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule dependenciesAreInjectedThroughConstructors =
      NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);
}
