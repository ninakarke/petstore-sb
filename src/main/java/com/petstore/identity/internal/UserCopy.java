package com.petstore.identity.internal;

import com.petstore.identity.internal.UserStore.StoredUser;
import com.petstore.shared.CopyReport;
import com.petstore.shared.StoreCopy;
import java.util.List;

/**
 * Copies all sign-in users from one store to another, through the identity port on both sides.
 * Password hashes are copied as they are; nobody has to choose a new password.
 */
public final class UserCopy {

  private UserCopy() {}

  public static List<CopyReport> copy(UserStore source, UserStore target, int batchSize) {
    return List.of(
        StoreCopy.copy(
            "users",
            StoredUser::username,
            source::usersFrom,
            target::usersFrom,
            user -> target.create(user.username(), user.passwordHash()),
            batchSize));
  }
}
