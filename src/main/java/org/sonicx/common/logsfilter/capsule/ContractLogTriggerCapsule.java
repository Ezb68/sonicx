package org.sonicx.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.sonicx.common.logsfilter.EventPluginLoader;
import org.sonicx.common.logsfilter.FilterQuery;
import org.sonicx.common.logsfilter.trigger.ContractLogTrigger;

public class ContractLogTriggerCapsule extends TriggerCapsule {
  @Getter
  @Setter
  ContractLogTrigger contractLogTrigger;

  public ContractLogTriggerCapsule(ContractLogTrigger contractLogTrigger) {
    this.contractLogTrigger = contractLogTrigger;
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    contractLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  @Override
  public void processTrigger() {
    if (FilterQuery.matchFilter(contractLogTrigger)) {
      EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
    }
  }
}
