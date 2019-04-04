package org.sonicx.core.exception;

public class VMMemoryOverflowException extends SonicxException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
