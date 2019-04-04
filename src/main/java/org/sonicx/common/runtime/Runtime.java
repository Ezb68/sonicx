package org.sonicx.common.runtime;

import lombok.Setter;
import org.sonicx.common.runtime.vm.program.InternalTransaction.TrxType;
import org.sonicx.common.runtime.vm.program.ProgramResult;
import org.sonicx.core.exception.ContractExeException;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.core.exception.VMIllegalException;


public interface Runtime {

  boolean isCallConstant() throws ContractValidateException;

  void execute() throws ContractValidateException, ContractExeException, VMIllegalException;

  void go();

  TrxType getTrxType();

  void finalization();

  ProgramResult getResult();

  String getRuntimeError();

  void setEnableEventLinstener(boolean enableEventLinstener);
}
