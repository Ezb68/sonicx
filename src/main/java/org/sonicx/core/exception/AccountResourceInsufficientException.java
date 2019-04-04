package org.sonicx.core.exception;

public class AccountResourceInsufficientException extends SonicxException {

  public AccountResourceInsufficientException() {
    super("Insufficient bandwidth and balance to create new account");
  }

  public AccountResourceInsufficientException(String message) {
    super(message);
  }
}

