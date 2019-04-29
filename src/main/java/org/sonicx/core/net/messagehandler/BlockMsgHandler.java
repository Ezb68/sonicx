package org.sonicx.core.net.messagehandler;

import static org.sonicx.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.sonicx.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import lombok.extern.slf4j.Slf4j;
import org.sonicx.core.net.SonicxNetDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.BlockCapsule.BlockId;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.exception.P2pException;
import org.sonicx.core.exception.P2pException.TypeEnum;
import org.sonicx.core.net.message.BlockMessage;
import org.sonicx.core.net.message.SonicxMessage;
import org.sonicx.core.net.peer.Item;
import org.sonicx.core.net.peer.PeerConnection;
import org.sonicx.core.net.service.AdvService;
import org.sonicx.core.net.service.SyncService;
import org.sonicx.core.services.WitnessProductBlockService;
import org.sonicx.protos.Protocol.Inventory.InventoryType;

@Slf4j
@Component
public class BlockMsgHandler implements SonicxMsgHandler {

  @Autowired
  private SonicxNetDelegate sonicxNetDelegate;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private WitnessProductBlockService witnessProductBlockService;

  private int maxBlockSize = BLOCK_SIZE + 1000;

  private boolean fastForward = Args.getInstance().isFastForward();

  @Override
  public void processMessage(PeerConnection peer, SonicxMessage msg) throws P2pException {

    BlockMessage blockMessage = (BlockMessage) msg;

    check(peer, blockMessage);

    BlockId blockId = blockMessage.getBlockId();
    Item item = new Item(blockId, InventoryType.BLOCK);
    if (peer.getSyncBlockRequested().containsKey(blockId)) {
      peer.getSyncBlockRequested().remove(blockId);
      syncService.processBlock(peer, blockMessage);
    } else {
      peer.getAdvInvRequest().remove(item);
      processBlock(peer, blockMessage.getBlockCapsule());
    }
  }

  private void check(PeerConnection peer, BlockMessage msg) throws P2pException {
    Item item = new Item(msg.getBlockId(), InventoryType.BLOCK);
    if (!peer.getSyncBlockRequested().containsKey(msg.getBlockId()) && !peer.getAdvInvRequest()
        .containsKey(item)) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "no request");
    }
    BlockCapsule blockCapsule = msg.getBlockCapsule();
    if (blockCapsule.getInstance().getSerializedSize() > maxBlockSize) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block size over limit");
    }
    long gap = blockCapsule.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new P2pException(TypeEnum.BAD_MESSAGE, "block time error");
    }
  }

  private void processBlock(PeerConnection peer, BlockCapsule block) throws P2pException {
    BlockId blockId = block.getBlockId();
    if (!sonicxNetDelegate.containBlock(block.getParentBlockId())) {
      logger.warn("Get unlink block {} from {}, head is {}.", blockId.getString(),
          peer.getInetAddress(), sonicxNetDelegate
              .getHeadBlockId().getString());
      syncService.startSync(peer);
      return;
    }

    if (fastForward && sonicxNetDelegate.validBlock(block)) {
      advService.broadcast(new BlockMessage(block));
    }

    sonicxNetDelegate.processBlock(block);
    witnessProductBlockService.validWitnessProductTwoBlock(block);
    sonicxNetDelegate.getActivePeer().forEach(p -> {
      if (p.getAdvInvReceive().getIfPresent(blockId) != null) {
        p.setBlockBothHave(blockId);
      }
    });

    if (!fastForward) {
      advService.broadcast(new BlockMessage(block));
    }
  }

}
