package org.sonicx.core.services.interfaceOnSolidity;

import org.springframework.stereotype.Component;
import org.sonicx.common.entity.NodeInfo;
import org.sonicx.core.services.NodeInfoService;

@Component
public class NodeInfoOnSolidityService extends NodeInfoService {

  @Override
  protected void setBlockInfo(NodeInfo nodeInfo) {
    super.setBlockInfo(nodeInfo);
    nodeInfo.setBlock(nodeInfo.getSolidityBlock());
    nodeInfo.setBeginSyncNum(-1);
  }

  @Override
  protected void setCheatWitnessInfo(NodeInfo nodeInfo) {
  }

}
