package org.sonicx.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
