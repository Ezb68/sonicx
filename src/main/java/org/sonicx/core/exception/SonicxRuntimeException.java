package org.sonicx.core.exception;

public class SonicxRuntimeException extends RuntimeException {

  public SonicxRuntimeException() {
    super();
  }

  public SonicxRuntimeException(String message) {
    super(message);
  }

  public SonicxRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public SonicxRuntimeException(Throwable cause) {
    super(cause);
  }

  protected SonicxRuntimeException(String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
