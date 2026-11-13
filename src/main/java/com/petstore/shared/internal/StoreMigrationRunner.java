package com.petstore.shared.internal;

import com.petstore.shared.CopyReport;
import com.petstore.shared.MigrationProperties;
import com.petstore.shared.StoreMigration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The job that copies the application's data from the relational database to MongoDB.
 *
 * <p>It runs every module's {@link StoreMigration} in order, prints one line per kind of record
 * saying how the two stores compare, and ends the process: exit code 0 if everything matches, 1 if
 * anything does not. It runs after the seed loaders, so an empty source is seeded first.
 */
@Component
@Profile(StoreMigration.PROFILE)
@Order(1000)
public class StoreMigrationRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StoreMigrationRunner.class);

  private final List<StoreMigration> migrations;
  private final MigrationProperties properties;
  private final ConfigurableApplicationContext context;

  StoreMigrationRunner(
      List<StoreMigration> migrations,
      MigrationProperties properties,
      ConfigurableApplicationContext context) {
    this.migrations =
        migrations.stream().sorted(Comparator.comparingInt(StoreMigration::order)).toList();
    this.properties = properties;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<CopyReport> reports = migrate();
    boolean allMatch = reports.stream().allMatch(CopyReport::matches);
    if (properties.exitWhenDone()) {
      System.exit(SpringApplication.exit(context, () -> allMatch ? 0 : 1));
    }
  }

  /** Runs all migrations and logs the comparison. Safe to call again. */
  public List<CopyReport> migrate() {
    List<CopyReport> reports = new ArrayList<>();
    for (StoreMigration migration : migrations) {
      reports.addAll(migration.run(properties.batchSize()));
    }
    log.info("Copy to MongoDB finished. Source and target compared:");
    log.info(
        String.format(
            "  %-14s %10s %8s %14s %10s %10s  %s",
            "kind", "in source", "copied", "already there", "in target", "different", "result"));
    for (CopyReport report : reports) {
      log.info(
          String.format(
              "  %-14s %10d %8d %14d %10d %10d  %s",
              report.what(),
              report.inSource(),
              report.copied(),
              report.alreadyThere(),
              report.inTarget(),
              report.different(),
              verdict(report)));
    }
    if (reports.stream().allMatch(CopyReport::matches)) {
      log.info("Result: the target holds exactly what the source holds.");
    } else {
      log.error("Result: the stores do NOT match. See the lines marked MISMATCH.");
    }
    return reports;
  }

  /** "match", or what is wrong: records that differ, or a different number of records. */
  private static String verdict(CopyReport report) {
    if (report.matches()) {
      return "match";
    }
    List<String> reasons = new ArrayList<>();
    if (report.different() > 0) {
      reasons.add(report.different() + " differ, for example " + report.examples());
    }
    if (report.inTarget() != report.inSource()) {
      reasons.add("target holds " + report.inTarget() + ", source " + report.inSource());
    }
    return "MISMATCH: " + String.join("; ", reasons);
  }
}
