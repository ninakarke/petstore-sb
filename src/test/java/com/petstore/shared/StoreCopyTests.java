package com.petstore.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.shared.StoreCopy.RangeReader;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The copy-and-compare routine, on two plain sorted maps standing in for data stores. */
class StoreCopyTests {

  private record Row(String key, String value) {}

  private final TreeMap<String, Row> source = new TreeMap<>();
  private final TreeMap<String, Row> target = new TreeMap<>();

  @ParameterizedTest(name = "batch size {0}")
  @ValueSource(ints = {1, 2, 3, 6, 7, 8, 100})
  @DisplayName("everything is copied whatever the batch size, and each record once")
  void copiesEverything(int batchSize) {
    sourceHolds("a", "b", "c", "d", "e", "f", "g");

    CopyReport report = copy(batchSize);

    assertThat(target).isEqualTo(source);
    assertThat(report.inSource()).isEqualTo(7);
    assertThat(report.copied()).isEqualTo(7);
    assertThat(report.alreadyThere()).isZero();
    assertThat(report.inTarget()).isEqualTo(7);
    assertThat(report.different()).isZero();
    assertThat(report.matches()).isTrue();
  }

  @Test
  @DisplayName("running it again copies nothing and still reports a match")
  void secondRunCopiesNothing() {
    sourceHolds("a", "b", "c");
    copy(2);

    CopyReport again = copy(2);

    assertThat(again.copied()).isZero();
    assertThat(again.alreadyThere()).isEqualTo(3);
    assertThat(again.matches()).isTrue();
  }

  @Test
  @DisplayName("only what the target lacks is written")
  void onlyMissingRecordsAreWritten() {
    sourceHolds("a", "b", "c", "d");
    target.put("b", source.get("b"));

    CopyReport report = copy(2);

    assertThat(report.copied()).isEqualTo(3);
    assertThat(report.alreadyThere()).isEqualTo(1);
    assertThat(report.matches()).isTrue();
  }

  @Test
  @DisplayName("a record that differs in the target is reported by key and is not overwritten")
  void differingRecordIsReportedNotOverwritten() {
    sourceHolds("a", "b", "c");
    target.put("b", new Row("b", "changed in the target"));

    CopyReport report = copy(10);

    assertThat(report.different()).isEqualTo(1);
    assertThat(report.examples()).containsExactly("b");
    assertThat(report.matches()).isFalse();
    assertThat(target.get("b").value()).isEqualTo("changed in the target");
  }

  @Test
  @DisplayName("a record the target has and the source does not is noticed")
  void extraRecordInTheTargetIsNoticed() {
    sourceHolds("a", "b");
    target.put("z", new Row("z", "only here"));

    CopyReport report = copy(10);

    assertThat(report.inSource()).isEqualTo(2);
    assertThat(report.inTarget()).isEqualTo(3);
    assertThat(report.different()).isZero();
    assertThat(report.matches()).isFalse();
  }

  @Test
  @DisplayName("what counts is what the target gives back, not what was sent to it")
  void writeThatStoresSomethingElseIsReported() {
    sourceHolds("a", "b");

    CopyReport report =
        StoreCopy.copy(
            "rows",
            Row::key,
            reader(source),
            reader(target),
            row -> target.put(row.key(), new Row(row.key(), row.value().substring(0, 3))),
            10);

    assertThat(report.copied()).isEqualTo(2);
    assertThat(report.different()).isEqualTo(2);
    assertThat(report.matches()).isFalse();
  }

  @Test
  @DisplayName("a write that stores nothing is reported")
  void writeThatStoresNothingIsReported() {
    sourceHolds("a");

    CopyReport report =
        StoreCopy.copy("rows", Row::key, reader(source), reader(target), row -> {}, 10);

    assertThat(report.different()).isEqualTo(1);
    assertThat(report.inTarget()).isZero();
    assertThat(report.matches()).isFalse();
  }

  @Test
  void emptySourceAndEmptyTargetMatch() {
    CopyReport report = copy(10);

    assertThat(report.inSource()).isZero();
    assertThat(report.matches()).isTrue();
  }

  @Test
  @DisplayName("the report names at most ten differing records, and counts them all")
  void examplesAreLimited() {
    for (int i = 10; i < 40; i++) {
      source.put("k" + i, new Row("k" + i, "source"));
      target.put("k" + i, new Row("k" + i, "target"));
    }

    CopyReport report = copy(7);

    assertThat(report.different()).isEqualTo(30);
    assertThat(report.examples()).hasSize(10).startsWith("k10", "k11");
  }

  @Test
  @DisplayName("a report about a single value carries the two values and the verdict")
  void valueReport() {
    CopyReport report = CopyReport.ofValue("counter", 3, 5, true);

    assertThat(report.inSource()).isEqualTo(3);
    assertThat(report.inTarget()).isEqualTo(5);
    assertThat(report.matches()).isTrue();
  }

  private void sourceHolds(String... keys) {
    for (String key : keys) {
      source.put(key, new Row(key, "value of " + key));
    }
  }

  private CopyReport copy(int batchSize) {
    return StoreCopy.copy(
        "rows",
        Row::key,
        reader(source),
        reader(target),
        row -> target.put(row.key(), row),
        batchSize);
  }

  private static RangeReader<Row> reader(TreeMap<String, Row> rows) {
    return (startKey, limit) ->
        (startKey == null ? rows : rows.tailMap(startKey, true))
            .values().stream().limit(limit).toList();
  }
}
