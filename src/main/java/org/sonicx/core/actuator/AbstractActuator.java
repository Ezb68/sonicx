package org.sonicx.core.actuator;

import com.google.protobuf.Any;
import org.sonicx.common.storage.Deposit;
import org.sonicx.core.capsule.TransactionResultCapsule;
import org.sonicx.core.db.Manager;
import org.sonicx.core.exception.ContractExeException;

public abstract class AbstractActuator implements Actuator {

  protected Any contract;
  protected Manager dbManager;

  public Deposit getDeposit() {
    return deposit;
  }

  public void setDeposit(Deposit deposit) {
    this.deposit = deposit;
  }

  protected Deposit deposit;

  AbstractActuator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }
}
