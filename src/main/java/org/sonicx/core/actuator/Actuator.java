package org.sonicx.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.sonicx.core.capsule.TransactionResultCapsule;
import org.sonicx.core.exception.ContractExeException;
import org.sonicx.core.exception.ContractValidateException;

public interface Actuator {

  boolean execute(TransactionResultCapsule result) throws ContractExeException;

  boolean validate() throws ContractValidateException;

  ByteString getOwnerAddress() throws InvalidProtocolBufferException;

  long calcFee();

}
