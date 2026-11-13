package com.petstore.catalog.internal.persistence.mongo;

import com.petstore.catalog.internal.CatalogCopy;
import com.petstore.catalog.internal.CatalogStore;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreMigration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/**
 * The catalog's part of the copy to MongoDB: from the application's catalog store, which in the
 * copy job is the relational one, into a MongoDB catalog store.
 */
@Component
@Profile(StoreMigration.PROFILE)
class MongoCatalogMigration implements StoreMigration {

  private final CatalogStore source;
  private final MongoCatalogStore target;

  MongoCatalogMigration(CatalogStore source, MongoOperations mongo) {
    this.source = source;
    this.target = new MongoCatalogStore(mongo);
    this.target.afterPropertiesSet();
  }

  @Override
  public int order() {
    return 10;
  }

  @Override
  public List<CopyReport> run(int batchSize) {
    return CatalogCopy.copy(source, target, batchSize);
  }
}
