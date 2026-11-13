package com.petstore.identity;

import com.petstore.identity.internal.UserStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Users: who may sign in, and how a new user is created.
 *
 * <p>Replaces the legacy {@code SignOnEJB} session bean and the creation rules of the {@code
 * UserEJB} entity bean.
 */
@Service
public class IdentityService {

  /** Legacy: {@code UserLocal.MAX_USERID_LENGTH}. */
  public static final int MAX_USERNAME_LENGTH = 25;

  /** Legacy: {@code UserLocal.MAX_PASSWD_LENGTH}. */
  public static final int MAX_PASSWORD_LENGTH = 25;

  private final UserStore users;
  private final PasswordEncoder passwordEncoder;

  public IdentityService(UserStore users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  public boolean exists(String username) {
    return username != null && users.find(username).isPresent();
  }

  /**
   * Creates a user.
   *
   * <p>Legacy rules, kept: the user name and the password are at most 25 characters long, and the
   * user name contains neither {@code %} nor {@code *}.
   *
   * <p>Differences from the legacy application:
   *
   * <ul>
   *   <li>the repeated password must match; the legacy code read it and never compared it;
   *   <li>user name and password may not be blank; the legacy code accepted empty values;
   *   <li>the password is stored as a salted hash, never as typed;
   *   <li>the caller learns what is wrong; the legacy code answered every failure with "the user
   *       name is in use".
   * </ul>
   *
   * @throws RegistrationException if a rule is broken or the user name is taken
   */
  public void register(String username, String password, String repeatedPassword) {
    if (username == null || username.isBlank()) {
      throw new RegistrationException(RegistrationProblem.USERNAME_REQUIRED);
    }
    if (password == null || password.isBlank()) {
      throw new RegistrationException(RegistrationProblem.PASSWORD_REQUIRED);
    }
    if (username.length() > MAX_USERNAME_LENGTH) {
      throw new RegistrationException(RegistrationProblem.USERNAME_TOO_LONG);
    }
    if (password.length() > MAX_PASSWORD_LENGTH) {
      throw new RegistrationException(RegistrationProblem.PASSWORD_TOO_LONG);
    }
    if (username.indexOf('%') != -1 || username.indexOf('*') != -1) {
      throw new RegistrationException(RegistrationProblem.USERNAME_HAS_FORBIDDEN_CHARACTER);
    }
    if (!password.equals(repeatedPassword)) {
      throw new RegistrationException(RegistrationProblem.PASSWORDS_DIFFER);
    }
    if (!users.create(username, passwordEncoder.encode(password))) {
      throw new RegistrationException(RegistrationProblem.USERNAME_TAKEN);
    }
  }
}
