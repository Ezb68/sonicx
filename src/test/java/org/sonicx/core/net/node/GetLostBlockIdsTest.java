package org.sonicx.core.net.node;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import io.grpc.Channel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.sonicx.common.overlay.server.ChannelManager;
import org.sonicx.common.overlay.server.SyncPool;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.common.utils.ReflectUtils;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.common.utils.Utils;
import org.sonicx.core.capsule.AccountCapsule;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.BlockCapsule.BlockId;
import org.sonicx.core.capsule.WitnessCapsule;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.ByteArrayWrapper;
import org.sonicx.core.db.Manager;
import org.sonicx.core.exception.StoreException;
import org.sonicx.core.net.node.override.HandshakeHandlerTest;
import org.sonicx.core.net.node.override.PeerClientTest;
import org.sonicx.core.net.node.override.SonicxChannelInitializerTest;
import org.sonicx.core.net.peer.PeerConnection;
import org.sonicx.core.services.RpcApiService;
import org.sonicx.core.services.WitnessService;
import org.sonicx.core.witness.WitnessController;
import org.sonicx.protos.Protocol;

@Slf4j
public class GetLostBlockIdsTest {

  private static SonicxApplicationContext context;
  private static NodeImpl node;
  private RpcApiService rpcApiService;
  private static PeerClientTest peerClient;
  private ChannelManager channelManager;
  private SyncPool pool;
  private static Application appT;
  private Manager dbManager;
  private Node nodeEntity;
  private static HandshakeHandlerTest handshakeHandlerTest;

  private static final String dbPath = "output-GetLostBlockIdsTest";
  private static final String dbDirectory = "db_GetLostBlockIds_test";
  private static final String indexDirectory = "index_GetLostBlockIds_test";

  @Test
  public void testGetLostBlockIds() {
    NodeDelegate del = ReflectUtils.getFieldValue(node, "del");
    List<BlockId> blockChainSummary;
    LinkedList<BlockId> blockIds = null;

    long number;
    Map<ByteString, String> addressToProvateKeys = addTestWitnessAndAccount();
    BlockCapsule capsule;
    for (int i = 0; i < 5; i++) {
      number = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;
      capsule = createTestBlockCapsule(1533529947843L + 3000L * i, number,
          dbManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
          addressToProvateKeys);
      try {
        dbManager.pushBlock(capsule);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    //blockChainSummary is empty
    try {
      blockChainSummary = new ArrayList<BlockId>();
      blockIds = del.getLostBlockIds(blockChainSummary);
    } catch (StoreException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(blockIds.size() == 6);

    //blockChainSummary only have a genesis block
    try {
      blockChainSummary = new ArrayList<BlockId>();
      blockChainSummary.add(dbManager.getGenesisBlockId());
      blockIds = del.getLostBlockIds(blockChainSummary);
    } catch (StoreException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(blockIds.size() == 6);

    //blockChainSummary have genesis block、2nd block、3rd block
    BlockId except_first_block = null;
    try {
      blockChainSummary = new ArrayList<BlockId>();
      blockChainSummary.add(dbManager.getGenesisBlockId());
      blockChainSummary.add(dbManager.getBlockIdByNum(2));
      blockChainSummary.add(dbManager.getBlockIdByNum(3));
      except_first_block = dbManager.getBlockIdByNum(3);
      blockIds = del.getLostBlockIds(blockChainSummary);
    } catch (StoreException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(blockIds.size() == 3 && Arrays
        .equals(blockIds.peekFirst().getBytes(), except_first_block.getBytes()));

    //blockChainSummary have 2nd block、4th block，and they are on fork chain
    try {
      BlockCapsule capsule2 = new BlockCapsule(2,
          Sha256Hash.wrap(ByteString.copyFrom(
              ByteArray.fromHexString(
                  "0000000000000002498b464ac0292229938a342238077182498b464ac0292222"))),
          1234, ByteString.copyFrom("1234567".getBytes()));
      BlockCapsule capsule4 = new BlockCapsule(4,
          Sha256Hash.wrap(ByteString.copyFrom(
              ByteArray.fromHexString(
                  "00000000000000042498b464ac0292229938a342238077182498b464ac029222"))),
          1234, ByteString.copyFrom("abcdefg".getBytes()));
      blockChainSummary = new ArrayList<BlockId>();
      blockChainSummary.add(capsule2.getBlockId());
      blockChainSummary.add(capsule4.getBlockId());
      blockIds = del.getLostBlockIds(blockChainSummary);
    } catch (StoreException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(blockIds.size() == 0);

    //blockChainSummary have 2nd block(main chain)、4th block(fork chain)
    try {
      BlockCapsule capsule4 = new BlockCapsule(4,
          Sha256Hash.wrap(ByteString.copyFrom(
              ByteArray.fromHexString(
                  "00000000000000042498b464ac0292229938a342238077182498b464ac029222"))),
          1234, ByteString.copyFrom("abcdefg".getBytes()));
      blockChainSummary = new ArrayList<BlockId>();
      blockChainSummary.add(dbManager.getBlockIdByNum(2));
      blockChainSummary.add(capsule4.getBlockId());
      except_first_block = dbManager.getBlockIdByNum(2);
      blockIds = del.getLostBlockIds(blockChainSummary);
    } catch (StoreException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(blockIds.size() == 4 && Arrays
        .equals(blockIds.peekFirst().getBytes(), except_first_block.getBytes()));
    logger.info("finish2");
  }

  private boolean go = false;

  private Map<ByteString, String> addTestWitnessAndAccount() {
    dbManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              dbManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              dbManager.getWitnessController().addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
              dbManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockCapsule createTestBlockCapsule(
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    long time = System.currentTimeMillis();
    return createTestBlockCapsule(time, number, hash, addressToProvateKeys);
  }

  private BlockCapsule createTestBlockCapsule(long time,
      long number, ByteString hash, Map<ByteString, String> addressToProvateKeys) {
    WitnessController witnessController = dbManager.getWitnessController();
    ByteString witnessAddress =
        witnessController.getScheduledWitness(witnessController.getSlotAtTime(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));
    return blockCapsule;
  }

  @Before
  public void init() {
    nodeEntity = new Node(
        "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17892");

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--output-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(17892);
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
        dbManager = context.getBean(Manager.class);
        handshakeHandlerTest = context.getBean(HandshakeHandlerTest.class);
        handshakeHandlerTest.setNode(nodeEntity);
        NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
        node.setNodeDelegate(nodeDelegate);
        pool.init(node);
        prepare();
        rpcApiService.blockUntilShutdown();
      }
    });
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
      org.sonicx.common.overlay.server.Channel channel = ReflectUtils
          .getFieldValue(sonicxChannelInitializer, "channel");
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
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
