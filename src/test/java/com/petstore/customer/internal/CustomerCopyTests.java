package com.petstore.customer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.customer.CustomerFixtures;
import com.petstore.shared.CopyReport;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Copying customers from one store to another, between two in-memory stores. */
class CustomerCopyTests {

  private final InMemoryCustomerStore source = new InMemoryCustomerStore();
  private final InMemoryCustomerStore target = new InMemoryCustomerStore();

  @Test
  void copiesCustomersComplete() {
    source.save(CustomerFixtures.customer("j2ee"));
    source.save(CustomerFixtures.customer("shopper"));
    source.save(CustomerFixtures.customer("duke"));

    List<CopyReport> reports = CustomerCopy.copy(source, target, 2);

    assertThat(reports)
        .singleElement()
        .satisfies(report -> {
          assertThat(report.what()).isEqualTo("customers");
          assertThat(report.inSource()).isEqualTo(3);
          assertThat(report.copied()).isEqualTo(3);
          assertThat(report.matches()).isTrue();
        });
    assertThat(target.find("duke")).contains(CustomerFixtures.customer("duke"));
    assertThat(CustomerCopy.copy(source, target, 2).get(0).copied()).isZero();
  }
}
