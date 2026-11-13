package com.petstore.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Copies the records of one kind from one data store to another and compares the two.
 *
 * <p>It knows nothing about any particular store or record: it is given a way to read records in
 * key order from each side and a way to write one to the target. Every persistence port offers
 * exactly that, so the same few lines move the catalog, the users, the customers and the orders,
 * between any two stores.
 *
 * <p>How it works, and why it is safe to run on a large store and to run twice:
 *
 * <ul>
 *   <li>the source is read a batch at a time, continuing after the last key seen, never with an
 *       offset, so the work per batch does not grow with the size of the store;
 *   <li>a record the target already holds is not written again;
 *   <li>every record, whether just written or found, is read back from the target and compared with
 *       the source, so the report says what the target really contains, not what was sent;
 *   <li>finally the target is counted, which shows records it holds that the source does not.
 * </ul>
 */
public final class StoreCopy {

  private static final int MAX_EXAMPLES = 10;

  private StoreCopy() {}

  /**
   * Reads records in the store's key order.
   *
   * @param <T> the kind of record
   */
  @FunctionalInterface
  public interface RangeReader<T> {

    /**
     * At most {@code limit} records, starting with the record of the given key, or with the first
     * record if the key is {@code null}. If no record has that key, the result starts with the next
     * one in key order.
     */
    List<T> from(String startKey, int limit);
  }

  /**
   * Copies every record of the source that the target does not hold, then compares.
   *
   * @param what the kind of record, used in the report
   * @param keyOf the key of a record
   * @param batchSize how many records to read at a time
   */
  public static <T> CopyReport copy(
      String what,
      Function<T, String> keyOf,
      RangeReader<T> source,
      RangeReader<T> target,
      Consumer<T> writeToTarget,
      int batchSize) {
    Tally tally = new Tally();
    long inSource =
        forEach(
            source,
            keyOf,
            batchSize,
            record -> {
              String key = keyOf.apply(record);
              Optional<T> inTarget = find(target, keyOf, key);
              if (inTarget.isEmpty()) {
                writeToTarget.accept(record);
                tally.copied++;
                inTarget = find(target, keyOf, key);
              } else {
                tally.alreadyThere++;
              }
              if (!inTarget.equals(Optional.of(record))) {
                tally.different++;
                if (tally.examples.size() < MAX_EXAMPLES) {
                  tally.examples.add(key);
                }
              }
            });
    long inTarget = forEach(target, keyOf, batchSize, record -> {});
    boolean matches = tally.different == 0 && inSource == inTarget;
    return new CopyReport(
        what,
        inSource,
        tally.copied,
        tally.alreadyThere,
        inTarget,
        tally.different,
        tally.examples,
        matches);
  }

  /** Visits every record of a store, a batch at a time, and returns how many there were. */
  private static <T> long forEach(
      RangeReader<T> reader, Function<T, String> keyOf, int batchSize, Consumer<T> action) {
    long count = 0;
    String lastKey = null;
    while (true) {
      // The start key is inclusive, so one record more is asked for than is wanted: every batch
      // but the first begins with the record the previous batch ended with.
      List<T> batch = reader.from(lastKey, batchSize + 1);
      boolean repeatsLast =
          lastKey != null && !batch.isEmpty() && keyOf.apply(batch.get(0)).equals(lastKey);
      List<T> fresh = repeatsLast ? batch.subList(1, batch.size()) : batch;
      for (T record : fresh) {
        action.accept(record);
        count++;
        lastKey = keyOf.apply(record);
      }
      if (batch.size() <= batchSize) {
        return count;
      }
    }
  }

  /** The record of a key, read through the same range reader: the first record at that key. */
  private static <T> Optional<T> find(
      RangeReader<T> reader, Function<T, String> keyOf, String key) {
    return reader.from(key, 1).stream()
        .filter(record -> keyOf.apply(record).equals(key))
        .findFirst();
  }

  private static final class Tally {
    private long copied;
    private long alreadyThere;
    private long different;
    private final List<String> examples = new ArrayList<>();
  }
}
