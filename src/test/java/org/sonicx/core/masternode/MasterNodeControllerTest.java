package org.sonicx.core.masternode;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.api.GrpcAPI;
import org.sonicx.common.application.ApplicationFactory;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.runtime.Runtime;
import org.sonicx.common.runtime.SVMTestUtils;
import org.sonicx.common.runtime.config.VMConfig;
import org.sonicx.common.runtime.vm.DataWord;
import org.sonicx.common.runtime.vm.program.ProgramResult;
import org.sonicx.common.storage.Deposit;
import org.sonicx.common.storage.DepositImpl;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.core.Constant;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.TransactionResultCapsule;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.Parameter.ForkBlockVersionConsts;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.Manager;
import org.sonicx.core.db.TransactionTrace;
import org.sonicx.core.exception.ContractExeException;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.core.exception.ReceiptCheckErrException;
import org.sonicx.core.exception.VMIllegalException;
import org.sonicx.core.mastrnode.MasterNodeController;
import org.sonicx.protos.Protocol.AccountType;
import org.sonicx.protos.Protocol.Transaction;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

@Slf4j
public class MasterNodeControllerTest {

    private Manager manager;
    final private byte[] zero = new DataWord(0).getData();
    private SonicxApplicationContext context;
    private String dbPath = "output_MasterNodeTest";
    private String ownerAddress;
    private String operatorAddress;
    private Deposit rootDeposit;
    final private byte[] one = new DataWord(1).getData();
    private MasterNodeController controller;

    @Before
    public void init() {
        Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
        Args.getInstance().getMasternode().setMinBlocksBeforeActivation(0);
        context = new SonicxApplicationContext(DefaultConfig.class);
        ownerAddress = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        operatorAddress = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

        manager = context.getBean(Manager.class);
        controller = context.getBean(MasterNodeController.class);
        rootDeposit = DepositImpl.createRoot(manager);
        rootDeposit.createAccount(Hex.decode(ownerAddress), AccountType.Normal);
        rootDeposit.addBalance(Hex.decode(ownerAddress), 30000000000000L);

        rootDeposit.createAccount(Hex.decode(operatorAddress), AccountType.Normal);
        rootDeposit.addBalance(Hex.decode(operatorAddress), 30000000000000L);

        rootDeposit.commit();

        // init
        //exec
        // TODO exception
        MasterNodeController.Triggerer triggerer = (triggerSmartContract, trxCap, builder, retBuilder) -> {
            rootDeposit.commit();
            TransactionTrace trace = new TransactionTrace(trxCap, rootDeposit.getDbManager());
            // init
            trace.init(null);
            //exec
            trace.exec();

            trace.finalization();

            // TODO exception
            if (trace.getRuntime().getResult().getException() != null) {
                RuntimeException e = trace.getRuntime().getResult().getException();
                logger.warn("Constant call has error {}", e.getMessage());
                throw e;
            }

            ProgramResult result = trace.getRuntime().getResult();
            TransactionResultCapsule ret = new TransactionResultCapsule();

            builder.addConstantResult(ByteString.copyFrom(result.getHReturn()));
            ret.setStatus(0, Transaction.Result.code.SUCESS);
            if (StringUtils.isNoneEmpty(trace.getRuntime().getRuntimeError())) {
                ret.setStatus(0, Transaction.Result.code.FAILED);
                retBuilder.setMessage(ByteString.copyFromUtf8(trace.getRuntime().getRuntimeError())).build();
            }
            trxCap.setResult(ret);
            rootDeposit.commit();

            return trxCap.getInstance();
        };

        controller.setTriggerer(triggerer);
    }

    public Transaction deploy() {
        byte[] stats = new byte[27];
        Arrays.fill(stats, (byte) 0);
        this.manager.getDynamicPropertiesStore()
                .statsByVersion(ForkBlockVersionConsts.ENERGY_LIMIT, stats);
        VMConfig.initVmHardFork();
        byte[] address = Hex.decode(ownerAddress);

        Transaction aTx = MasterNodeController.deploy(address, 32000L);

        Runtime runtime = null;
        try {
            runtime = SVMTestUtils.processTransactionAndReturnRuntime(aTx, rootDeposit, null);
        } catch (ContractExeException | VMIllegalException | ReceiptCheckErrException | ContractValidateException e) {
            Assert.assertNull(e);
        }

        assert runtime != null;
        Assert.assertNull(runtime.getRuntimeError());

        rootDeposit.commit();

        return aTx;
    }

    public GrpcAPI.TransactionExtention announceMasternode(byte[] contractAddress, byte[] ownerAddress, byte[] operatorAddress,
                                                           long operatorRewardRatio, long feeLimit, long callValue) {
        String owner = ByteArray.toHexString(ownerAddress);
        String operator = ByteArray.toHexString(operatorAddress);

        return controller.announceMasternode(contractAddress, ownerAddress, feeLimit, callValue, owner,
                operator, operator, operatorRewardRatio);
    }

    @Test
    public void deployTest() {
        Transaction aTx = deploy();
        byte[] masternodeAddress = Wallet.generateContractAddress(aTx);

        GrpcAPI.TransactionExtention result = controller.getMasternodesNum(masternodeAddress);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.SUCESS);
    }

    @Test
    public void announceMasternodeTest() {
        Transaction aTx = deploy();

        byte[] masternodeAddress = Wallet.generateContractAddress(aTx);
        byte[] caller = Hex.decode(ownerAddress);
        byte[] caller2 = Hex.decode(operatorAddress);
        long feeLimit = 100000000L;
        long operatorRewardRatio = 300000L;
        long callValue = 10000000000000L;
        long lessValue = callValue - 1;

        String owner = ByteArray.toHexString(caller);
        String operator = ByteArray.toHexString(caller2);

        GrpcAPI.TransactionExtention result = controller.getMasternodesNum(masternodeAddress);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), zero);

        result = controller.mnOperatorIndexes(masternodeAddress, operator);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), zero);

        result = controller.mnOwnerIndexes(masternodeAddress, owner);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), zero);

        result = controller.wholeCollateral(masternodeAddress);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), zero);

        result = announceMasternode(masternodeAddress, caller, caller2,
                operatorRewardRatio, feeLimit, lessValue);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.FAILED);

        result = announceMasternode(masternodeAddress, caller, caller2,
                operatorRewardRatio, feeLimit, callValue);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.SUCESS);

        result = controller.getMasternodesNum(masternodeAddress);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), one);

        result = controller.mnOperatorIndexes(masternodeAddress, operator);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), one);

        MasterNodeController.MasternodesArrayResults masterNode1 = controller.masternodesArray(
                masternodeAddress, new BigInteger(result.getConstantResult(0).toByteArray()));
        Assert.assertNotNull(masterNode1);

        result = controller.mnOwnerIndexes(masternodeAddress, owner);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), one);

        MasterNodeController.MasternodesArrayResults masterNode2 = controller.masternodesArray(
                masternodeAddress, new BigInteger(result.getConstantResult(0).toByteArray()));
        Assert.assertNotNull(masterNode2);

        Assert.assertEquals(masterNode2, masterNode1);

        result = controller.wholeCollateral(masternodeAddress);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), new DataWord(callValue).getData());
    }

    @Test
    public void activateMasternodeTest() {
        Transaction aTx = deploy();

        byte[] masternodeAddress = Wallet.generateContractAddress(aTx);
        byte[] caller = Hex.decode(ownerAddress);
        byte[] caller2 = Hex.decode(operatorAddress);
        long feeLimit = 100000000L;
        long operatorRewardRatio = 300000L;
        long callValue = 10000000000000L;

        GrpcAPI.TransactionExtention result = announceMasternode(masternodeAddress, caller, caller2,
                operatorRewardRatio, feeLimit, callValue);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.SUCESS);

        result = controller.activateMasternode(masternodeAddress, caller, 100000000L);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.FAILED);

        result = controller.activateMasternode(masternodeAddress, caller2, 100000000L);
        Assert.assertEquals(result.getTransaction().getRet(0).getRet(), Transaction.Result.code.SUCESS);

        result = controller.numOfActivatedMasternodes(masternodeAddress);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), one);

        result = controller.wholeActivatedCollateral(masternodeAddress);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), new DataWord(callValue).getData());

        result = controller.getMasternodesHistorySize(masternodeAddress);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), one);

        MasterNodeController.MnsHistoryResults history = controller.mnsHistory(masternodeAddress,
                new BigInteger(zero.clone()));

        Assert.assertNotNull(history);
        Assert.assertEquals(history.BlockNumber, new BigInteger(zero));
        Assert.assertEquals(history.WholeActivatedCollateral.longValue(), callValue);
        Assert.assertEquals(history.NumOfActivatedMasternodes.longValue(), 1L);
        Assert.assertEquals(history.RewardsPerBlock.longValue(), 32000L);
    }

    @Test
    public void minBlocksBeforeMnActivationTest() {
        Transaction aTx = deploy();
        byte[] masternodeAddress = Wallet.generateContractAddress(aTx);

        GrpcAPI.TransactionExtention result = controller.minBlocksBeforeMnActivation(masternodeAddress);
        Assert.assertTrue(result.getResult().getResult());
        Assert.assertEquals(result.getConstantResultCount(), 1);
        Assert.assertEquals(result.getConstantResult(0).toByteArray(), new DataWord(0).getData());
    }

    @After
    public void destroy() {
        Args.clearParam();
        ApplicationFactory.create(context).shutdown();
        ApplicationFactory.create(context).shutdownServices();
        context.destroy();
        if (FileUtil.deleteDir(new File(dbPath))) {
            logger.info("Release resources successful.");
        } else {
            logger.error("Release resources failure.");
        }
    }

}
