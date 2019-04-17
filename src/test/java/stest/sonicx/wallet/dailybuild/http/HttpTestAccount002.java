package stest.sonicx.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Utils;
import stest.sonicx.wallet.common.client.Configuration;
import stest.sonicx.wallet.common.client.utils.HttpMethed;
import stest.sonicx.wallet.common.client.utils.PublicMethed;

@Slf4j
public class HttpTestAccount002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] freezeBalanceAddress = ecKey1.getAddress();
  String freezeBalanceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverResourceAddress = ecKey2.getAddress();
  String receiverResourceKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long berforeBalance;
  Long afterBalance;

  Long amount = 10000000L;
  Long frozenBalance = 2000000L;

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for bandwidth by http")
  public void test1FreezebalanceForBandwidth() {
    PublicMethed.printAddress(freezeBalanceKey);
    //Send sox to test account
    response = HttpMethed.sendCoin(httpnode,fromAddress,freezeBalanceAddress,amount,testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //Freeze balance
    response = HttpMethed.freezeBalance(httpnode,freezeBalanceAddress,frozenBalance,0,
        0,freezeBalanceKey);
    Assert.assertEquals(response.getStatusLine().getStatusCode(),200);
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for bandwidth by http")
  public void test2UnFreezebalanceForBandwidth() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //UnFreeze balance for bandwidth
    response = HttpMethed.unFreezeBalance(httpnode,freezeBalanceAddress,0,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance for energy by http")
  public void test3FreezebalanceForEnergy() {
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //Freeze balance for energy
    response = HttpMethed.freezeBalance(httpnode,freezeBalanceAddress,frozenBalance,0,
        1,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance for energy by http")
  public void test4UnFreezebalanceForEnergy() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //UnFreeze balance for energy
    response = HttpMethed.unFreezeBalance(httpnode,freezeBalanceAddress,1,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with bandwidth for others by http")
  public void test5FreezebalanceOfBandwidthForOthers() {
    response = HttpMethed.sendCoin(httpnode,fromAddress,receiverResourceAddress,amount,testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //Freeze balance with bandwidth for others
    response = HttpMethed.freezeBalance(httpnode,freezeBalanceAddress,frozenBalance,0,
        0,receiverResourceAddress,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with bandwidth for others by http")
  public void test6UnFreezebalanceOfBandwidthForOthers() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //UnFreeze balance with bandwidth for others
    response = HttpMethed.unFreezeBalance(httpnode,freezeBalanceAddress,0,
        receiverResourceAddress,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "FreezeBalance with energy for others by http")
  public void test7FreezebalanceOfEnergyForOthers() {
    response = HttpMethed.sendCoin(httpnode,fromAddress,receiverResourceAddress,amount,testKey002);
    Assert.assertTrue(HttpMethed.verificationResult(response));

    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //Freeze balance with energy for others
    response = HttpMethed.freezeBalance(httpnode,freezeBalanceAddress,frozenBalance,0,
        1,receiverResourceAddress,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(berforeBalance - afterBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "UnFreezeBalance with energy for others by http")
  public void test8UnFreezebalanceOfEnergyForOthers() {
    HttpMethed.waitToProduceOneBlock(httpnode);
    berforeBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);

    //UnFreeze balance with energy for others
    response = HttpMethed.unFreezeBalance(httpnode,freezeBalanceAddress,1,
        receiverResourceAddress,freezeBalanceKey);
    Assert.assertTrue(HttpMethed.verificationResult(response));
    afterBalance = HttpMethed.getBalance(httpnode,freezeBalanceAddress);
    Assert.assertTrue(afterBalance - berforeBalance == frozenBalance);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    HttpMethed.disConnect();
  }
}
