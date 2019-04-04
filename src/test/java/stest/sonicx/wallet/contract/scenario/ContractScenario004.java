package stest.sonicx.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.sonicx.api.GrpcAPI.AccountResourceMessage;
import org.sonicx.api.WalletGrpc;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Utils;
import org.sonicx.core.Wallet;
import org.sonicx.protos.Protocol.SmartContract;
import stest.sonicx.wallet.common.client.Configuration;
import stest.sonicx.wallet.common.client.Parameter.CommonConstant;
import stest.sonicx.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractScenario004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract004Address = ecKey1.getAddress();
  String contract004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract004Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void deployErc20SonicxToken() {
    Assert.assertTrue(PublicMethed.sendcoin(contract004Address,200000000L,fromAddress,
        testKey002,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(contract004Address, 100000000L,
        3,1,contract004Key,blockingStubFull));
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract004Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    String contractName = "SONICXTOKEN";
    String code = Configuration.getByPath("testng.conf")
            .getString("code.code_ContractScenario004_deployErc20SonicxToken");
    String abi = Configuration.getByPath("testng.conf")
            .getString("abi.abi_ContractScenario004_deployErc20SonicxToken");
    byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",maxFeeLimit,
        0L,100,null,contract004Key,contract004Address,blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress,blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    accountResource = PublicMethed.getAccountResource(contract004Address,blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);
    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
  }
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}

