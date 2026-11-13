package com.petstore.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.identity.internal.UserStore.StoredUser;
import com.petstore.shared.CopyReport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Copying users from one store to another, between two in-memory stores. */
class UserCopyTests {

  private final InMemoryUserStore source = new InMemoryUserStore();
  private final InMemoryUserStore target = new InMemoryUserStore();

  @Test
  @DisplayName("users are copied with their password hashes as they are")
  void copiesUsersWithTheirHashes() {
    source.create("j2ee", "{bcrypt}hash-1");
    source.create("shopper", "{bcrypt}hash-2");
    source.create("duke", "{bcrypt}hash-3");

    List<CopyReport> reports = UserCopy.copy(source, target, 2);

    assertThat(reports)
        .singleElement()
        .satisfies(report -> {
          assertThat(report.what()).isEqualTo("users");
          assertThat(report.copied()).isEqualTo(3);
          assertThat(report.matches()).isTrue();
        });
    assertThat(target.find("shopper")).contains(new StoredUser("shopper", "{bcrypt}hash-2"));
  }

  @Test
  @DisplayName("a user whose name is taken in the target by another password is reported")
  void reportsAUserThatDiffers() {
    source.create("j2ee", "{bcrypt}hash-1");
    target.create("j2ee", "{bcrypt}another");

    CopyReport report = UserCopy.copy(source, target, 10).get(0);

    assertThat(report.matches()).isFalse();
    assertThat(report.examples()).containsExactly("j2ee");
    assertThat(target.find("j2ee")).contains(new StoredUser("j2ee", "{bcrypt}another"));
  }
}
