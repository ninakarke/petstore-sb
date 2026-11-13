package com.petstore.shared.internal.persistence.mongo;

import com.petstore.shared.DocumentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * What all MongoDB adapters have in common: how objects become documents.
 *
 * <p>Each module's adapter declares its own document classes; those classes are the schema of its
 * collections. Nothing here knows about any of them.
 */
@Configuration(proxyBeanMethods = false)
@DocumentStore
public class MongoStoreConfiguration {

  /**
   * The converter Spring Boot would create, with one difference: documents are written without a
   * {@code _class} field.
   *
   * <p>That field names the Java class a document was written from. It is only needed to read
   * documents back into a class hierarchy, which this application does not have, and it would tie
   * the stored data to the names of Java classes: renaming or moving a class would then be a data
   * migration.
   */
  @Bean
  MappingMongoConverter mappingMongoConverter(
      MongoDatabaseFactory databaseFactory,
      MongoMappingContext mappingContext,
      MongoCustomConversions conversions) {
    MappingMongoConverter converter =
        new MappingMongoConverter(new DefaultDbRefResolver(databaseFactory), mappingContext);
    converter.setCustomConversions(conversions);
    converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    return converter;
  }
}
