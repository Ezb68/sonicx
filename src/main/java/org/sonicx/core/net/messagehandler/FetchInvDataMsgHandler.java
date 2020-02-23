package org.sonicx.core.net.messagehandler;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonicx.common.overlay.discover.node.statistics.MessageCount;
import org.sonicx.common.overlay.message.Message;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.capsule.BlockCapsule.BlockId;
import org.sonicx.core.config.Parameter.ChainConstant;
import org.sonicx.core.config.Parameter.NodeConstant;
import org.sonicx.core.exception.P2pException;
import org.sonicx.core.exception.P2pException.TypeEnum;
import org.sonicx.core.net.SonicxNetDelegate;
import org.sonicx.core.net.message.BlockMessage;
import org.sonicx.core.net.message.FetchInvDataMessage;
import org.sonicx.core.net.message.MessageTypes;
import org.sonicx.core.net.message.TransactionMessage;
import org.sonicx.core.net.message.TransactionsMessage;
import org.sonicx.core.net.message.SonicxMessage;
import org.sonicx.core.net.peer.Item;
import org.sonicx.core.net.peer.PeerConnection;
import org.sonicx.core.net.service.AdvService;
import org.sonicx.core.net.service.SyncService;
import org.sonicx.protos.Protocol.Inventory.InventoryType;
import org.sonicx.protos.Protocol.ReasonCode;
import org.sonicx.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements SonicxMsgHandler {

  @Autowired
  private SonicxNetDelegate sonicxNetDelegate;

  @Autowired
  private SyncService syncService;

  @Autowired
  private AdvService advService;

  private int MAX_SIZE = 1_000_000;

  @Override
  public void processMessage(PeerConnection peer, SonicxMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = sonicxNetDelegate.getData(hash, type);
        } catch (Exception e) {
          logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type.equals(InventoryType.BLOCK)) {
        BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance()
            .getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (transactions.size() > 0) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.TRX) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.TRX)) == null) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.sonicxInTrxFetchInvDataElement
          .getCount(10);
      int maxCount = advService.getTrxCount().getCount(60);
      if (fetchCount > maxCount) {
        throw new P2pException(TypeEnum.BAD_MESSAGE,
            "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount sonicxOutAdvBlock = peer.getNodeStatistics().messageStatistics.sonicxOutAdvBlock;
        sonicxOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = sonicxOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / ChainConstant.BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: " + producedBlockIn2min
              + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * NodeConstant.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                new BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}
