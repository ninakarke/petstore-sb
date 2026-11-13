package com.petstore.identity;

/** Why a registration was refused. */
public enum RegistrationProblem {
  USERNAME_REQUIRED,
  PASSWORD_REQUIRED,
  USERNAME_TOO_LONG,
  PASSWORD_TOO_LONG,
  USERNAME_HAS_FORBIDDEN_CHARACTER,
  PASSWORDS_DIFFER,
  USERNAME_TAKEN
}
