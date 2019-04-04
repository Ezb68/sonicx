package org.sonicx.core.net.node;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonicx.common.overlay.message.Message;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.BlockCapsule.BlockId;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.exception.BadBlockException;
import org.sonicx.core.exception.BadTransactionException;
import org.sonicx.core.exception.NonCommonBlockException;
import org.sonicx.core.exception.StoreException;
import org.sonicx.core.exception.SonicxException;
import org.sonicx.core.exception.UnLinkedBlockException;
import org.sonicx.core.net.message.MessageTypes;

public interface NodeDelegate {

  LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException;

  boolean handleTransaction(TransactionCapsule trx) throws BadTransactionException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws SonicxException;

  Message getData(Sha256Hash msgId, MessageTypes type) throws StoreException;

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);

  BlockId getHeadBlockId();

  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockCapsule getGenesisBlock();

  boolean canChainRevoke(long num);
}
