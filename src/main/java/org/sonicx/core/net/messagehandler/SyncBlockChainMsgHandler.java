package org.sonicx.core.net.messagehandler;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.sonicx.core.net.SonicxNetDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.core.capsule.BlockCapsule.BlockId;
import org.sonicx.core.config.Parameter.NodeConstant;
import org.sonicx.core.exception.P2pException;
import org.sonicx.core.exception.P2pException.TypeEnum;
import org.sonicx.core.net.message.ChainInventoryMessage;
import org.sonicx.core.net.message.SyncBlockChainMessage;
import org.sonicx.core.net.message.SonicxMessage;
import org.sonicx.core.net.peer.PeerConnection;

@Slf4j
@Component
public class SyncBlockChainMsgHandler implements SonicxMsgHandler {

  @Autowired
  private SonicxNetDelegate sonicxNetDelegate;

  @Override
  public void processMessage(PeerConnection peer, SonicxMessage msg) throws P2pException {

    SyncBlockChainMessage syncBlockChainMessage = (SyncBlockChainMessage) msg;

    check(peer, syncBlockChainMessage);

    long remainNum = 0;

    List<BlockId> summaryChainIds = syncBlockChainMessage.getBlockIds();

    LinkedList<BlockId> blockIds = getLostBlockIds(summaryChainIds);

    if (blockIds.size() == 1) {
      peer.setNeedSyncFromUs(false);
    } else {
      peer.setNeedSyncFromUs(true);
      remainNum = sonicxNetDelegate.getHeadBlockId().getNum() - blockIds.peekLast().getNum();
    }
//
//    if (!peer.isNeedSyncFromPeer()
//        && !sonicxNetDelegate.contain(Iterables.getLast(summaryChainIds), MessageTypes.BLOCK)
//        && sonicxNetDelegate.canChainRevoke(summaryChainIds.get(0).getNum())) {
//      //startSyncWithPeer(peer);
//    }

    peer.setLastSyncBlockId(blockIds.peekLast());
    peer.setRemainNum(remainNum);
    peer.sendMessage(new ChainInventoryMessage(blockIds, remainNum));
  }

  private void check(PeerConnection peer, SyncBlockChainMessage msg) throws P2pException {
    List<BlockId> blockIds = msg.getBlockIds();
    if (CollectionUtils.isEmpty(blockIds)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "SyncBlockChain blockIds is empty");
    }

    BlockId firstId = blockIds.get(0);
    if (!sonicxNetDelegate.containBlockInMainChain(firstId)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "No first block:" + firstId.getString());
    }

    long headNum = sonicxNetDelegate.getHeadBlockId().getNum();
    if (firstId.getNum() > headNum) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "First blockNum:" + firstId.getNum() + " gt my head BlockNum:" + headNum);
    }

    BlockId lastSyncBlockId = peer.getLastSyncBlockId();
    long lastNum = blockIds.get(blockIds.size() - 1).getNum();
    if (lastSyncBlockId != null && lastSyncBlockId.getNum() > lastNum) {
      throw new P2pException(TypeEnum.BAD_MESSAGE,
          "lastSyncNum:" + lastSyncBlockId.getNum() + " gt lastNum:" + lastNum);
    }
  }

  private LinkedList<BlockId> getLostBlockIds(List<BlockId> blockIds) throws P2pException {

    BlockId unForkId = null;
    for (int i = blockIds.size() - 1; i >= 0; i--) {
      if (sonicxNetDelegate.containBlockInMainChain(blockIds.get(i))) {
        unForkId = blockIds.get(i);
        break;
      }
    }

    long len = Math.min(sonicxNetDelegate.getHeadBlockId().getNum(),
        unForkId.getNum() + NodeConstant.SYNC_FETCH_BATCH_NUM);

    LinkedList<BlockId> ids = new LinkedList<>();
    for (long i = unForkId.getNum(); i <= len; i++) {
      BlockId id = sonicxNetDelegate.getBlockIdByNum(i);
      ids.add(id);
    }
    return ids;
  }

}