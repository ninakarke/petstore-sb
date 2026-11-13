package com.petstore.order.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The contract every {@link OrderIdGenerator} adapter must fulfil.
 *
 * <p>Legacy sources: {@code components/uidgen/.../ejb/UniqueIdGeneratorEJB.java} and {@code
 * .../counter/ejb/CounterEJB.java} ({@code getName() + String.valueOf(count)}), called by {@code
 * OrderEJBAction} as {@code getUniqueId("1001")}.
 */
public abstract class OrderIdGeneratorContract {

  /** The adapter under test. */
  protected abstract OrderIdGenerator generator();

  /** Puts the counter back to its starting point. */
  protected abstract void resetTheCounter();

  @BeforeEach
  void startFromZero() {
    resetTheCounter();
  }

  @Test
  @DisplayName("legacy: order ids are 1001 followed by a counter that starts at one")
  void orderIdsFollowTheLegacyFormat() {
    assertThat(generator().nextOrderId()).isEqualTo("10011");
    assertThat(generator().nextOrderId()).isEqualTo("10012");
    assertThat(generator().nextOrderId()).isEqualTo("10013");
  }

  @Test
  @DisplayName("legacy: the tenth order is 100110, plain concatenation")
  void counterIsAppendedWithoutPadding() {
    String last = null;
    for (int i = 0; i < 10; i++) {
      last = generator().nextOrderId();
    }

    assertThat(last).isEqualTo("100110");
  }

  @Test
  @DisplayName("fixed: requests arriving at the same time never get the same number")
  void concurrentRequestsGetDifferentNumbers() throws Exception {
    int threads = 8;
    int callsPerThread = 5;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Callable<List<String>>> tasks = new ArrayList<>();
      for (int t = 0; t < threads; t++) {
        tasks.add(() -> {
          List<String> ids = new ArrayList<>();
          for (int i = 0; i < callsPerThread; i++) {
            ids.add(generator().nextOrderId());
          }
          return ids;
        });
      }
      List<String> all = new ArrayList<>();
      for (Future<List<String>> result : pool.invokeAll(tasks, 60, TimeUnit.SECONDS)) {
        all.addAll(result.get());
      }

      assertThat(all).hasSize(threads * callsPerThread).doesNotHaveDuplicates();
      // Nothing was skipped either: the next number follows directly.
      assertThat(generator().nextOrderId()).isEqualTo("1001" + (threads * callsPerThread + 1));
    } finally {
      pool.shutdownNow();
    }
  }

  // ---- continuing the numbering of another store ---------------------------------------------

  @Test
  void lastNumberIsTheCounterOfTheLastOrderId() {
    assertThat(generator().lastNumber()).isZero();

    generator().nextOrderId();
    generator().nextOrderId();

    assertThat(generator().lastNumber()).isEqualTo(2);
  }

  @Test
  @DisplayName("after orders are copied in, numbering continues above them and never moves back")
  void advancingSkipsNumbersAlreadyInUse() {
    generator().advanceTo(41);
    assertThat(generator().nextOrderId()).isEqualTo("100142");

    generator().advanceTo(7);
    assertThat(generator().nextOrderId()).isEqualTo("100143");
  }
}
