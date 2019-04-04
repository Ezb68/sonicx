package org.sonicx.core.exception;

public class TooBigTransactionResultException extends SonicxException {

    public TooBigTransactionResultException() { super("too big transaction result"); }

    public TooBigTransactionResultException(String message) { super(message); }
}
