package org.sonicx.core.net.node;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.application.Application;
import org.sonicx.common.application.ApplicationFactory;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.overlay.discover.node.Node;
import org.sonicx.common.overlay.server.Channel;
import org.sonicx.common.overlay.server.ChannelManager;
import org.sonicx.common.overlay.server.SyncPool;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.common.utils.ReflectUtils;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.Constant;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.utils.BlockUtil;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.ByteArrayWrapper;
import org.sonicx.core.db.Manager;
import org.sonicx.core.net.message.BlockMessage;
import org.sonicx.core.net.node.override.HandshakeHandlerTest;
import org.sonicx.core.net.node.override.PeerClientTest;
import org.sonicx.core.net.node.override.SonicxChannelInitializerTest;
import org.sonicx.core.net.peer.PeerConnection;
import org.sonicx.core.services.RpcApiService;
import org.sonicx.core.services.WitnessService;
import org.sonicx.protos.Protocol;

@Slf4j
public class HandleSyncBlockTest {

  private static SonicxApplicationContext context;
  private static NodeImpl node;
  private RpcApiService rpcApiService;
  private static PeerClientTest peerClient;
  private ChannelManager channelManager;
  private SyncPool pool;
  private static Application appT;
  private Node nodeEntity;
  private static HandshakeHandlerTest handshakeHandlerTest;
  private static final String dbPath = "output-nodeImplTest-handleSyncBlock";
  private static final String dbDirectory = "db_HandleSyncBlock_test";
  private static final String indexDirectory = "index_HandleSyncBlock_test";

  private class Condition {

    private Sha256Hash blockId;

    public Condition(Sha256Hash blockId) {
      this.blockId = blockId;
    }

    public Sha256Hash getBlockId() {
      return blockId;
    }
  }

  private Sha256Hash testBlockBroad() {
    Protocol.Block block = Protocol.Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(new BlockCapsule(block));
    node.broadcast(blockMessage);
    ConcurrentHashMap<Sha256Hash, Protocol.Inventory.InventoryType> advObjToSpread = ReflectUtils
        .getFieldValue(node, "advObjToSpread");
    Assert.assertEquals(advObjToSpread.get(blockMessage.getMessageId()),
        Protocol.Inventory.InventoryType.BLOCK);
    return blockMessage.getMessageId();
  }

  private Condition testConsumerAdvObjToSpread() {
    Sha256Hash blockId = testBlockBroad();

    ReflectUtils.invokeMethod(node, "consumerAdvObjToSpread");
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");

    boolean result = true;
    for (PeerConnection peerConnection : activePeers) {
      if (!peerConnection.getAdvObjWeSpread().containsKey(blockId)) {
        result &= false;
      }
    }
    for (PeerConnection peerConnection : activePeers) {
      peerConnection.getAdvObjWeSpread().clear();
    }
    Assert.assertTrue(result);
    return new Condition(blockId);
  }

  private BlockMessage buildBlockMessage() throws Exception {
    BlockCapsule genesisBlockCapsule = BlockUtil.newGenesisBlockCapsule();

    ByteString witnessAddress = ByteString.copyFrom(
        ECKey.fromPrivate(
            ByteArray.fromHexString(
                Args.getInstance().getLocalWitnesses().getPrivateKey()))
            .getAddress());
    Protocol.BlockHeader.raw raw = Protocol.BlockHeader.raw.newBuilder()
        .setTimestamp(System.currentTimeMillis())
        .setParentHash(genesisBlockCapsule.getBlockId().getByteString())
        .setNumber(genesisBlockCapsule.getNum() + 1)
        .setWitnessAddress(witnessAddress)
        .setWitnessId(1).build();
    Protocol.BlockHeader blockHeader = Protocol.BlockHeader.newBuilder()
        .setRawData(raw)
        .build();

    Protocol.Block block = Protocol.Block.newBuilder().setBlockHeader(blockHeader).build();

    BlockCapsule blockCapsule = new BlockCapsule(block);
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
    BlockMessage blockMessage = new BlockMessage(blockCapsule);
    return blockMessage;
  }

  @Test
  public void testHandleSyncBlock() throws Exception {
    testConsumerAdvObjToSpread();
    // build block Message
    BlockMessage blockMessage = buildBlockMessage();
    // build blockJustReceived
    Map<BlockMessage, PeerConnection> blockJustReceived = new ConcurrentHashMap<>();
    blockJustReceived.put(blockMessage, new PeerConnection());
    ReflectUtils.setFieldValue(node, "blockJustReceived", blockJustReceived);
    Thread.sleep(1000);
    // retrieve the first active peer
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");
    activePeers.iterator().next().getSyncBlockToFetch().push(blockMessage.getBlockId());
    // clean up freshBlockId
    Queue<BlockCapsule.BlockId> freshBlockId = ReflectUtils
        .getFieldValue(node, "freshBlockId");
    freshBlockId.poll();
    // trigger handlesyncBlock method
    ReflectUtils.invokeMethod(node, "handleSyncBlock");

    Assert.assertTrue(freshBlockId.contains(blockMessage.getBlockId()));
  }

  private static boolean go = false;

  @Before
  public void init() {
    nodeEntity = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17893");

    new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--output-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            Constant.TEST_CONF
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(17893);
        cfgArgs.setNodeDiscoveryEnable(false);
        cfgArgs.getSeedNode().getIpList().clear();
        cfgArgs.setNeedSyncCheck(false);
        cfgArgs.setNodeExternalIp("127.0.0.1");

        context = new SonicxApplicationContext(DefaultConfig.class);

        if (cfgArgs.isHelp()) {
          logger.info("Here is the help message.");
          return;
        }
        appT = ApplicationFactory.create(context);
        rpcApiService = context.getBean(RpcApiService.class);
        appT.addService(rpcApiService);
        if (cfgArgs.isWitness()) {
          appT.addService(new WitnessService(appT, context));
        }
//        appT.initServices(cfgArgs);
//        appT.startServices();
//        appT.startup();
        node = context.getBean(NodeImpl.class);
        peerClient = context.getBean(PeerClientTest.class);
        channelManager = context.getBean(ChannelManager.class);
        pool = context.getBean(SyncPool.class);
        Manager dbManager = context.getBean(Manager.class);
        handshakeHandlerTest = context.getBean(HandshakeHandlerTest.class);
        handshakeHandlerTest.setNode(nodeEntity);
        NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
        node.setNodeDelegate(nodeDelegate);
        pool.init(node);
        prepare();
        rpcApiService.blockUntilShutdown();
      }
    }).start();
    int tryTimes = 0;
    while (tryTimes < 10 && (node == null || peerClient == null
        || channelManager == null || pool == null || !go)) {
      try {
        logger.info("node:{},peerClient:{},channelManager:{},pool:{},{}", node, peerClient,
            channelManager, pool, go);
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        ++tryTimes;
      }
    }
  }

  private void prepare() {
    try {
      ExecutorService advertiseLoopThread = ReflectUtils.getFieldValue(node, "broadPool");
      advertiseLoopThread.shutdownNow();

      peerClient.prepare(nodeEntity.getHexId());

      ReflectUtils.setFieldValue(node, "isAdvertiseActive", false);
      ReflectUtils.setFieldValue(node, "isFetchActive", false);

      SonicxChannelInitializerTest sonicxChannelInitializer = ReflectUtils
          .getFieldValue(peerClient, "sonicxChannelInitializer");
      sonicxChannelInitializer.prepare();
      Channel channel = ReflectUtils.getFieldValue(sonicxChannelInitializer, "channel");
      ReflectUtils.setFieldValue(channel, "handshakeHandler", handshakeHandlerTest);

      new Thread(new Runnable() {
        @Override
        public void run() {
          peerClient.connect(nodeEntity.getHost(), nodeEntity.getPort(), nodeEntity.getHexId());
        }
      }).start();
      Thread.sleep(1000);
      Map<ByteArrayWrapper, Channel> activePeers = ReflectUtils
          .getFieldValue(channelManager, "activePeers");
      int tryTimes = 0;
      while (MapUtils.isEmpty(activePeers) && ++tryTimes < 10) {
        Thread.sleep(1000);
      }
      go = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    Collection<PeerConnection> peerConnections = ReflectUtils.invokeMethod(node, "getActivePeer");
    for (PeerConnection peer : peerConnections) {
      peer.close();
    }
    handshakeHandlerTest.close();
    context.destroy();
    appT.shutdownServices();
    appT.shutdown();
    FileUtil.deleteDir(new File(dbPath));
  }
}
