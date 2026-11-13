package com.petstore.shared;

import java.util.List;

/**
 * One module's part of copying the application's data from the relational database to MongoDB.
 *
 * <p>Each module contributes one, because only the module knows its own persistence port. They are
 * run together by a one-off job that is started with the {@link #PROFILE} profile; see {@code
 * application-migrate-to-mongo.yml}.
 */
public interface StoreMigration {

  /** The profile that turns the application into the copy job. */
  String PROFILE = "migrate-to-mongo";

  /** Lower runs first: data is copied before the data that refers to it. */
  int order();

  /** Copies this module's data and reports on every kind of record. */
  List<CopyReport> run(int batchSize);
}
