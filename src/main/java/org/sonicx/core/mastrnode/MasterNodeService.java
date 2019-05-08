package org.sonicx.core.mastrnode;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.sonicx.api.GrpcAPI;
import org.sonicx.api.GrpcAPI.TransactionExtention;
import org.sonicx.common.application.Application;
import org.sonicx.common.application.Service;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.crypto.ECKey;
import org.sonicx.common.runtime.vm.DataWord;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.config.Parameter;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.Manager;
import org.sonicx.protos.Protocol;

import java.math.BigInteger;
import java.util.Arrays;

@Slf4j(topic = "masternode")
public class MasterNodeService implements Service {

    private byte[] operatorKey;
    private byte[] oparatorAddress;

    private Thread generateThread;

    private volatile boolean isRunning = false;

    private SonicxApplicationContext context;

    private Application sonicxApp;

    private MasterNodeController controller;

    private Manager manager;

    private Wallet wallet;

    long delta;


    private byte[] zero = new DataWord("0000000000000000000000000000000000000000000000000000000000000000").getData();
    private Runnable scheduleLoop =
            () -> {
                while (isRunning) {
                    try {
                        DateTime time = DateTime.now();
                        long timeToNextSecond = Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL
                                - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                                % Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
                        if (timeToNextSecond < 50L) {
                            timeToNextSecond = timeToNextSecond + Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
                        }
                        DateTime nextTime = time.plus(timeToNextSecond);
                        logger.debug(
                                "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
                        Thread.sleep(timeToNextSecond);

                        this.handler();

                    } catch (InterruptedException e) {
                        logger.info("ScheduleLoop interrupted");
                    }
                }
            };

    public MasterNodeService(Application sonicxApp, SonicxApplicationContext context) {
        this.sonicxApp = sonicxApp;
        this.context = context;

        generateThread = new Thread(scheduleLoop);
        controller = context.getBean(MasterNodeController.class);
        manager = context.getBean(Manager.class);
        wallet = context.getBean(Wallet.class);
        delta = Args.getInstance().getMasternode().getRewardsPeriod();
    }

    private boolean isSuccessTransaction(TransactionExtention te) {
        if (te == null || te.getResult() == null) {
            return false;
        }

        return te.getResult().getCodeValue() == Protocol.Transaction.Result.code.SUCESS.getNumber();
    }

    private byte[] genTxID(Protocol.Transaction transaction) {
        return Sha256Hash.hash(transaction.getRawData().toByteArray());
    }

    private void handler() {
        if (manager.getMastrnodesContractAddress() == null) {
            return;
        }

        TransactionExtention te = controller.mnOperatorIndexes(manager.getMastrnodesContractAddress(),
                ByteArray.toHexString(oparatorAddress));

        if (!isSuccessTransaction(te)) {
            return;
        }

        if (te.getConstantResultCount() == 0 ||
                Arrays.equals(te.getConstantResult(0).toByteArray(), zero)) {
            return;
        }

        BigInteger arg = new BigInteger(te.getConstantResult(0).toByteArray());

        MasterNodeController.MasternodesArrayResults masterNode = controller.masternodesArray(
                manager.getMastrnodesContractAddress(), arg);

        if (masterNode == null) {
            return;
        }

        long activationBlockNumber = masterNode.ActivationBlockNumber.longValue();

        if (activationBlockNumber != 0 && activationBlockNumber < manager.getHeadBlockNum()) {
            BigInteger nextBlock = masterNode.PrevRewardBlockNumber.add(BigInteger.valueOf(delta));
            if (nextBlock.compareTo(BigInteger.valueOf(manager.getHeadBlockNum())) < 0) {
                logger.info("give me my money, prevent reward block number equal: {}, current block {}",
                        masterNode.PrevRewardBlockNumber, manager.getHeadBlockNum());
                takeRewards();
            }
        }
    }

    private void takeRewards() {
        TransactionExtention te = controller.claimMasternodeReward(manager.getMastrnodesContractAddress(),
                oparatorAddress, 10000000L);

        if (!isSuccessTransaction(te) || !te.hasTransaction()) {
            return;
        }

        // Sign the transaction.
        TransactionCapsule transactionCapsule = new TransactionCapsule(te.getTransaction());
        transactionCapsule.sign(operatorKey);

        // Try to broadcast the transaction.
        GrpcAPI.Return ret = wallet.broadcastTransaction(transactionCapsule.getInstance());
        byte[] txID = genTxID(transactionCapsule.getInstance());

        if (!ret.getResult()) {
            logger.warn("failed to broadcast transaction: {}", ByteArray.toHexString(txID));
            return;
        }

        waitMining(txID);
    }

    private void waitMining(byte[] txID) {
        long limit = 60; // TODO: The magic number of iterations.
        long errors = 0;
        long delay = 1000; // in milliseconds.

        while (isRunning) {
            if (errors > limit) {
                logger.warn("the transaction {} was not mined", ByteArray.toHexString(txID));
                break;
            }

            try {
                Protocol.TransactionInfo tr = wallet.getTransactionInfoById(ByteString.copyFrom(txID));
                if (tr == null || tr.getReceipt().getResult() == Protocol.Transaction.Result.contractResult.DEFAULT ||
                        tr.getReceipt().getResult() == Protocol.Transaction.Result.contractResult.UNRECOGNIZED) {
                    Thread.sleep(delay);
                    errors++;
                    continue;
                }

                if (tr.getReceipt().getResult() != Protocol.Transaction.Result.contractResult.SUCCESS) {
                    logger.warn("the transaction {} is failed: {}",
                            ByteArray.toHexString(txID), tr.getReceipt().getResult().toString());
                }
                break;
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void init() {
        if (!Args.getInstance().getMasternode().isEnable() ||
                Args.getInstance().getMasternode().getOperatorPrivateKey() == null ||
                Args.getInstance().getMasternode().getOperatorPrivateKey().length() == 0) {
            return;
        }

        operatorKey = ByteArray.fromHexString(Args.getInstance().getMasternode().getOperatorPrivateKey());
        oparatorAddress = ECKey.fromPrivate(operatorKey).getAddress();
    }

    @Override
    public void init(Args args) {
        init();
    }

    @Override
    public void start() {
        if (operatorKey == null || oparatorAddress == null || operatorKey.length == 0) {
            logger.warn("the operator not defined");
            return;
        }
        isRunning = true;
        generateThread.start();
    }

    @Override
    public void stop() {
        isRunning = false;
        generateThread.interrupt();
    }
}
