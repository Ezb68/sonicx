package org.sonicx.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.sonicx.common.net.udp.message.UdpMessageTypeEnum;
import org.sonicx.common.overlay.message.Message;
import org.sonicx.core.net.message.FetchInvDataMessage;
import org.sonicx.core.net.message.InventoryMessage;
import org.sonicx.core.net.message.MessageTypes;
import org.sonicx.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp sonicx
  public final MessageCount sonicxInMessage = new MessageCount();
  public final MessageCount sonicxOutMessage = new MessageCount();

  public final MessageCount sonicxInSyncBlockChain = new MessageCount();
  public final MessageCount sonicxOutSyncBlockChain = new MessageCount();
  public final MessageCount sonicxInBlockChainInventory = new MessageCount();
  public final MessageCount sonicxOutBlockChainInventory = new MessageCount();

  public final MessageCount sonicxInTrxInventory = new MessageCount();
  public final MessageCount sonicxOutTrxInventory = new MessageCount();
  public final MessageCount sonicxInTrxInventoryElement = new MessageCount();
  public final MessageCount sonicxOutTrxInventoryElement = new MessageCount();

  public final MessageCount sonicxInBlockInventory = new MessageCount();
  public final MessageCount sonicxOutBlockInventory = new MessageCount();
  public final MessageCount sonicxInBlockInventoryElement = new MessageCount();
  public final MessageCount sonicxOutBlockInventoryElement = new MessageCount();

  public final MessageCount sonicxInTrxFetchInvData = new MessageCount();
  public final MessageCount sonicxOutTrxFetchInvData = new MessageCount();
  public final MessageCount sonicxInTrxFetchInvDataElement = new MessageCount();
  public final MessageCount sonicxOutTrxFetchInvDataElement = new MessageCount();

  public final MessageCount sonicxInBlockFetchInvData = new MessageCount();
  public final MessageCount sonicxOutBlockFetchInvData = new MessageCount();
  public final MessageCount sonicxInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount sonicxOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount sonicxInTrx = new MessageCount();
  public final MessageCount sonicxOutTrx = new MessageCount();
  public final MessageCount sonicxInTrxs = new MessageCount();
  public final MessageCount sonicxOutTrxs = new MessageCount();
  public final MessageCount sonicxInBlock = new MessageCount();
  public final MessageCount sonicxOutBlock = new MessageCount();
  public final MessageCount sonicxOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      sonicxInMessage.add();
    } else {
      sonicxOutMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          sonicxInSyncBlockChain.add();
        } else {
          sonicxOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          sonicxInBlockChainInventory.add();
        } else {
          sonicxOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        if (flag) {
          if (inventoryMessage.getInvMessageType() == MessageTypes.TRX) {
            sonicxInTrxInventory.add();
            sonicxInTrxInventoryElement.add(inventorySize);
          } else {
            sonicxInBlockInventory.add();
            sonicxInBlockInventoryElement.add(inventorySize);
          }
        } else {
          if (inventoryMessage.getInvMessageType() == MessageTypes.TRX) {
            sonicxOutTrxInventory.add();
            sonicxOutTrxInventoryElement.add(inventorySize);
          } else {
            sonicxOutBlockInventory.add();
            sonicxOutBlockInventoryElement.add(inventorySize);
          }
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        if (flag) {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TRX) {
            sonicxInTrxFetchInvData.add();
            sonicxInTrxFetchInvDataElement.add(fetchSize);
          } else {
            sonicxInBlockFetchInvData.add();
            sonicxInBlockFetchInvDataElement.add(fetchSize);
          }
        } else {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.TRX) {
            sonicxOutTrxFetchInvData.add();
            sonicxOutTrxFetchInvDataElement.add(fetchSize);
          } else {
            sonicxOutBlockFetchInvData.add();
            sonicxOutBlockFetchInvDataElement.add(fetchSize);
          }
        }
        break;
      case TRXS:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          sonicxInTrxs.add();
          sonicxInTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          sonicxOutTrxs.add();
          sonicxOutTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case TRX:
        if (flag) {
          sonicxInMessage.add();
        } else {
          sonicxOutMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          sonicxInBlock.add();
        }
        sonicxOutBlock.add();
        break;
      default:
        break;
    }
  }

}
