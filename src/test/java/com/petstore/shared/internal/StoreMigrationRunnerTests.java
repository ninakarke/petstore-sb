package com.petstore.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.shared.CopyReport;
import com.petstore.shared.MigrationProperties;
import com.petstore.shared.StoreMigration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * The copy job's runner on its own, with stand-in migrations: the order they run in and what the
 * report says. The copying itself is tested in {@code StoreCopyTests} and, for real databases, in
 * the {@code migration} tests.
 */
@ExtendWith(OutputCaptureExtension.class)
class StoreMigrationRunnerTests {

  private final List<String> ran = new ArrayList<>();

  @Test
  @DisplayName("modules are copied in dependency order, whatever order they are registered in")
  void runsInOrder() {
    StoreMigrationRunner runner =
        runner(migration(40, matching("orders")), migration(10, matching("categories")));

    List<CopyReport> reports = runner.migrate();

    assertThat(ran).containsExactly("categories", "orders");
    assertThat(reports).extracting(CopyReport::what).containsExactly("categories", "orders");
  }

  @Test
  @DisplayName("the configured batch size is handed to every module")
  void passesTheBatchSize() {
    List<Integer> sizes = new ArrayList<>();
    StoreMigration recording =
        new StoreMigration() {
          @Override
          public int order() {
            return 1;
          }

          @Override
          public List<CopyReport> run(int batchSize) {
            sizes.add(batchSize);
            return List.of();
          }
        };

    new StoreMigrationRunner(List.of(recording), new MigrationProperties(250, false), null)
        .run(null);

    assertThat(sizes).containsExactly(250);
  }

  @Test
  @DisplayName("when everything matches the report says so")
  void reportsAMatch(CapturedOutput output) {
    runner(migration(10, matching("categories"))).migrate();

    assertThat(output)
        .contains("categories")
        .contains("match")
        .contains("the target holds exactly what the source holds")
        .doesNotContain("MISMATCH");
  }

  @Test
  @DisplayName(
      "a mismatch is reported with its reason: which records differ, or how many are missing")
  void reportsWhatIsWrong(CapturedOutput output) {
    CopyReport differing = new CopyReport("customers", 4, 0, 4, 4, 1, List.of("j2ee"), false);
    CopyReport extra = new CopyReport("orders", 2, 0, 2, 3, 0, List.of(), false);

    runner(migration(30, differing), migration(40, extra)).migrate();

    assertThat(output)
        .contains("MISMATCH: 1 differ, for example [j2ee]")
        .contains("MISMATCH: target holds 3, source 2")
        .contains("the stores do NOT match");
  }

  private StoreMigrationRunner runner(StoreMigration... migrations) {
    return new StoreMigrationRunner(List.of(migrations), new MigrationProperties(500, false), null);
  }

  private StoreMigration migration(int order, CopyReport report) {
    return new StoreMigration() {
      @Override
      public int order() {
        return order;
      }

      @Override
      public List<CopyReport> run(int batchSize) {
        ran.add(report.what());
        return List.of(report);
      }
    };
  }

  private static CopyReport matching(String what) {
    return new CopyReport(what, 3, 3, 0, 3, 0, List.of(), true);
  }
}
