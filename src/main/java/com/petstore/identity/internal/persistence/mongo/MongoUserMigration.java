package com.petstore.identity.internal.persistence.mongo;

import com.petstore.identity.internal.UserCopy;
import com.petstore.identity.internal.UserStore;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreMigration;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

/** The identity module's part of the copy to MongoDB. */
@Component
@Profile(StoreMigration.PROFILE)
class MongoUserMigration implements StoreMigration {

  private final UserStore source;
  private final MongoUserStore target;

  MongoUserMigration(UserStore source, MongoOperations mongo) {
    this.source = source;
    this.target = new MongoUserStore(mongo);
  }

  @Override
  public int order() {
    return 20;
  }

  @Override
  public List<CopyReport> run(int batchSize) {
    return UserCopy.copy(source, target, batchSize);
  }
}
