package org.sonicx.core.exception;

public class SonicxException extends Exception {

  public SonicxException() {
    super();
  }

  public SonicxException(String message) {
    super(message);
  }

  public SonicxException(String message, Throwable cause) {
    super(message, cause);
  }

}
