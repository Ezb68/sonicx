package org.sonicx.core.services;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.sonicx.api.GrpcAPI.EmptyMessage;
import org.sonicx.api.WalletGrpc;
import org.sonicx.api.WalletGrpc.WalletBlockingStub;
import org.sonicx.common.entity.NodeInfo;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.net.node.BaseNetTest;
import org.sonicx.program.Version;
import stest.sonicx.wallet.common.client.Configuration;

@Slf4j
public class NodeInfoServiceTest extends BaseNetTest {

  private static final String dbPath = "output-nodeImplTest-nodeinfo";
  private static final String dbDirectory = "db_nodeinfo_test";
  private static final String indexDirectory = "index_nodeinfo_test";
  private final static int port = 15899;

  private NodeInfoService nodeInfoService;
  private WitnessProductBlockService witnessProductBlockService;

  public NodeInfoServiceTest() {
    super(dbPath, dbDirectory, indexDirectory, port);
  }

  @Test
  public void test() {
    nodeInfoService = context.getBean("nodeInfoService", NodeInfoService.class);
    witnessProductBlockService = context.getBean(WitnessProductBlockService.class);
    BlockCapsule blockCapsule1 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        100, ByteString.EMPTY);
    BlockCapsule blockCapsule2 = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
        200, ByteString.EMPTY);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule1);
    witnessProductBlockService.validWitnessProductTwoBlock(blockCapsule2);
    NodeInfo nodeInfo = nodeInfoService.getNodeInfo();
    Assert.assertEquals(nodeInfo.getConfigNodeInfo().getCodeVersion(), Version.getVersion());
    Assert.assertEquals(nodeInfo.getCheatWitnessInfoMap().size(), 1);
    logger.info("{}", JSON.toJSONString(nodeInfo));
  }

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  public void testGrpc() {
    WalletBlockingStub walletStub = WalletGrpc.newBlockingStub(ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build());
    logger.info("getNodeInfo: {}",walletStub.getNodeInfo(EmptyMessage.getDefaultInstance()));
  }

  public static void main(String[] args) {
    NodeInfoServiceTest test = new NodeInfoServiceTest();
    test.testGrpc();
  }
}
