package com.petstore.identity;

/** Thrown when a new user cannot be created. Carries the reason. */
public class RegistrationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final RegistrationProblem problem;

  public RegistrationException(RegistrationProblem problem) {
    super("Registration refused: " + problem);
    this.problem = problem;
  }

  public RegistrationProblem problem() {
    return problem;
  }
}
