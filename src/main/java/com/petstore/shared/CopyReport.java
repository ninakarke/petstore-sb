package com.petstore.shared;

import java.util.List;

/**
 * What happened when the records of one kind were copied from one data store to another, and how
 * the two stores compare afterwards.
 *
 * @param what the kind of record, for example "orders"
 * @param inSource how many records the source holds
 * @param copied how many were written to the target in this run
 * @param alreadyThere how many the target held before this run
 * @param inTarget how many records the target holds now
 * @param different how many records read back from the target differently from the source
 * @param examples keys of some of the differing records
 * @param matches whether the target now holds what the source holds
 */
public record CopyReport(
    String what,
    long inSource,
    long copied,
    long alreadyThere,
    long inTarget,
    long different,
    List<String> examples,
    boolean matches) {

  public CopyReport {
    examples = List.copyOf(examples);
  }

  /** A report about one value, such as a counter, instead of a set of records. */
  public static CopyReport ofValue(
      String what, long sourceValue, long targetValue, boolean matches) {
    return new CopyReport(what, sourceValue, 0, 0, targetValue, 0, List.of(), matches);
  }
}
