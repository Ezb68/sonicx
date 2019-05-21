

package org.sonicx.core.mastrnode;

import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.api.GrpcAPI;
import org.sonicx.api.GrpcAPI.TransactionExtention;
import org.sonicx.common.runtime.utils.Abi;
import org.sonicx.common.runtime.vm.DataWord;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.Manager;
import org.sonicx.core.exception.ContractExeException;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.core.exception.HeaderNotFound;
import org.sonicx.core.exception.VMIllegalException;
import org.sonicx.core.services.http.JsonFormat;
import org.sonicx.protos.Contract;
import org.sonicx.protos.Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;

@Slf4j(topic = "masternode")
@Component
public class MasterNodeController {

    public static final String contractName = "MasterNodesContract";
    private static final String byteCode = Args.getInstance().getMasternode().getByteCode();
    private static final String abiJson = Args.getInstance().getMasternode().getAbi();
    private static final long minBlocksBeforeActivation = Args.getInstance().getMasternode().getMinBlocksBeforeActivation();
    // Contract methods.
    private static final String getMasternodesHistorySize = "getMasternodesHistorySize";
    private static final String minimumCollateral = "minimumCollateral";
    private static final String getMasternodesNum = "getMasternodesNum";
    private static final String announceMasternode = "announceMasternode";
    private static final String getMasternodeRewardInfo = "getMasternodeRewardInfo";
    private static final String getMasternodeRewardPerBlock = "getMasternodeRewardPerBlock";
    private static final String getOperatorReward = "getOperatorReward";
    private static final String masternodesArray = "masternodesArray";
    private static final String masternodesRewardsPerBlock = "masternodesRewardsPerBlock";
    private static final String maxMnMetadataLen = "maxMnMetadataLen";
    private static final String maxRewardHistoryHops = "maxRewardHistoryHops";
    private static final String maxRewardRatio = "maxRewardRatio";
    private static final String minBlocksBeforeMnActivation = "minBlocksBeforeMnActivation";
    private static final String mnOperatorIndexes = "mnOperatorIndexes";
    private static final String mnOwnerIndexes = "mnOwnerIndexes";
    private static final String mnsHistory = "mnsHistory";
    private static final String numOfActivatedMasternodes = "numOfActivatedMasternodes";
    private static final String proposalsCounter = "proposalsCounter";
    private static final String wholeActivatedCollateral = "wholeActivatedCollateral";
    private static final String wholeCollateral = "wholeCollateral";
    private static final String activateMasternode = "activateMasternode";
    private static final String claimMasternodeReward = "claimMasternodeReward";
    private static final String resign = "resign";
    private static final String setMasternodeMetadata = "setMasternodeMetadata";
    private static final String setOperator = "setOperator";
    private static final String currentRewardsPerBlock = "currentRewardsPerBlock";
    @Autowired
    private Manager dbManager;
    @Autowired
    private Wallet wallet;
    private Abi abi;
    @Setter
    private Triggerer triggerer;

    /**
     * Construction method.
     */
    public MasterNodeController() {
        abi = Abi.fromJson(abiJson);
        triggerer = (triggerSmartContract, trxCap, builder, retBuilder) -> wallet.triggerContract(triggerSmartContract,
                trxCap, builder, retBuilder);
    }

    public static Protocol.Transaction deploy(byte[] ownerAddress, long rewardsPerBlock, long minimumCollateral) {
        final long value = 0;
        final long feeLimit = 1000000000;
        final long originEnergyLimit = 1000000;
        final long consumeUserResourcePercent = 0;

        Protocol.SmartContract.ABI.Builder abiBuilder = Protocol.SmartContract.ABI.newBuilder();
        String abiSB = "{" + "\"entrys\":" + abiJson + "}";

        try {
            JsonFormat.merge(abiSB, abiBuilder);
        } catch (JsonFormat.ParseException e) {
            return null;
        }

        String minBlocksBeforeActivationArg = new DataWord(minBlocksBeforeActivation).toHexString();
        String rewardsPerBlockArg = new DataWord(rewardsPerBlock).toHexString();
        String minimumCollateralArg = new DataWord(minimumCollateral).toHexString();

        String byteCodeWithArgs = byteCode + minBlocksBeforeActivationArg + rewardsPerBlockArg + minimumCollateralArg;
        byte[] code = ByteArray.fromHexString(byteCodeWithArgs);

        Protocol.SmartContract.Builder smartBuilder = Protocol.SmartContract.newBuilder();
        smartBuilder
                .setName(contractName)
                .setOriginAddress(ByteString.copyFrom(ownerAddress))
                .setAbi(abiBuilder)
                .setBytecode(ByteString.copyFrom(code))
                .setCallValue(value)
                .setConsumeUserResourcePercent(consumeUserResourcePercent)
                .setOriginEnergyLimit(originEnergyLimit);

        Contract.CreateSmartContract.Builder build = Contract.CreateSmartContract.newBuilder();
        build
                .setOwnerAddress(ByteString.copyFrom(ownerAddress))
                .setNewContract(smartBuilder);

        TransactionCapsule trxCapWithoutFeeLimit = new TransactionCapsule(build.build(),
                Protocol.Transaction.Contract.ContractType.CreateSmartContract);

        Protocol.Transaction.Builder txBuilder = trxCapWithoutFeeLimit
                .getInstance().toBuilder();
        Protocol.Transaction.raw.Builder rawBuilder = trxCapWithoutFeeLimit
                .getInstance().getRawData().toBuilder();

        //long currentTime = System.currentTimeMillis();
        //rawBuilder.setTimestamp(currentTime);
        rawBuilder.setFeeLimit(feeLimit);
        txBuilder.setRawData(rawBuilder);

        return txBuilder.build();
    }

    private TransactionExtention proc(byte[] contractAddress, byte[] callerAddress, String method,
                                      long feeLimit, long callValue, Object... args) {
        Abi.Function func = abi.getFunction(method);
        byte[] triggerData = func.encode(args);

        Contract.TriggerSmartContract.Builder build = Contract.TriggerSmartContract.newBuilder();
        GrpcAPI.TransactionExtention.Builder trxExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
        GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();

        try {
            build.setCallValue(callValue);
            build.setData(ByteString.copyFrom(triggerData));
            build.setContractAddress(ByteString.copyFrom(contractAddress));

            if (null != callerAddress) {
                build.setOwnerAddress(ByteString.copyFrom(callerAddress));
            }

            TransactionCapsule trxCap = new TransactionCapsule(
                    build.build(), Protocol.Transaction.Contract.ContractType.TriggerSmartContract);

            BlockCapsule.BlockId blockId = dbManager.getHeadBlockId();
            if (Args.getInstance().getTrxReferenceBlock().equals("solid")) {
                blockId = dbManager.getSolidBlockId();
            }
            trxCap.setReference(blockId.getNum(), blockId.getBytes());
            long expiration =
                    dbManager.getHeadBlockTimeStamp() + Args.getInstance()
                            .getTrxExpirationTimeInMilliseconds();
            trxCap.setExpiration(expiration);
            trxCap.setTimestamp();

            Protocol.Transaction.Builder txBuilder = trxCap.getInstance().toBuilder();
            Protocol.Transaction.raw.Builder rawBuilder = trxCap.getInstance().getRawData().toBuilder();
            rawBuilder.setFeeLimit(feeLimit);
            txBuilder.setRawData(rawBuilder);

            Protocol.Transaction trx = triggerer.triggerContract(build.build(),
                    new TransactionCapsule(txBuilder.build()), trxExtBuilder, retBuilder);
            trxExtBuilder.setTransaction(trx);
            retBuilder.setResult(true).setCode(GrpcAPI.Return.response_code.SUCCESS);
        } catch (ContractValidateException e) {
            retBuilder.setResult(false).setCode(GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getMessage()));
        } catch (Exception e) {
            retBuilder.setResult(false).setCode(GrpcAPI.Return.response_code.OTHER_ERROR)
                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        }

        trxExtBuilder.setResult(retBuilder);
        return trxExtBuilder.build();
    }

    public byte[] getGenesisContractAddress() {
        return dbManager.getMastrnodesContractAddress();
    }

    // getMasternodesHistorySize is a free data retrieval call binding the contract method 0x93b5809b.
    // Solidity: function getMasternodesHistorySize() constant returns(uint256)
    public TransactionExtention getMasternodesHistorySize(byte[] contractAddressHex) {
        return proc(contractAddressHex, null, getMasternodesHistorySize, 0L, 0L);
    }

    // minimumCollateral is a free data retrieval call binding the contract method 0xbabe7c74.
    //
    // Solidity: function minimumCollateral() constant returns(uint256)
    public TransactionExtention minimumCollateral(byte[] contractAddressHex) {
        return proc(contractAddressHex, null, minimumCollateral, 0L, 0L);
    }

    // getMasternodesNum is a free data retrieval call binding the contract method 0xef329a0c.
    //
    // Solidity: function getMasternodesNum() constant returns(uint256)
    public TransactionExtention getMasternodesNum(byte[] contractAddressHex) {
        return proc(contractAddressHex, null, getMasternodesNum, 0L, 0L);
    }

    // announceMasternode is a paid mutator transaction binding the contract method 0x1affd684.
    //
    // Solidity: function announceMasternode(address ownerRewardAddress, address operator,
    // address operatorRewardAddress, uint256 operatorRewardRatio) returns()
    public TransactionExtention announceMasternode(byte[] contractAddress, byte[] callerAddress, long feeLimit,
                                                   long callValue, String ownerRewardAddress, String operator,
                                                   String operatorRewardAddress, long operatorRewardRatio) {
        return proc(contractAddress, callerAddress, announceMasternode, feeLimit, callValue, ownerRewardAddress,
                operator, operatorRewardAddress, operatorRewardRatio);
    }

    // getMasternodeRewardInfo is a free data retrieval call binding the contract method 0x35ac2027.
    //
    // Solidity: function getMasternodeRewardInfo(address owner, uint256 rewardHeight) constant
    // returns(uint256 operatorReward, uint256 ownerReward)
    public TransactionExtention getMasternodeRewardInfo(byte[] contractAddress, byte[] callerAddress, long feeLimit,
                                                        long callValue, String owner, BigInteger rewardHeight) {
        return proc(contractAddress, callerAddress, getMasternodeRewardInfo, feeLimit, callValue, owner, rewardHeight);
    }

    // getMasternodeRewardPerBlock is a free data retrieval call binding the contract method 0xeb1a7e2e.
    // Solidity: function getMasternodeRewardPerBlock(uint256 collateralAmount, uint256 _wholeActivatedCollateral)
    // constant returns(uint256)
    public TransactionExtention getMasternodeRewardPerBlock(byte[] contractAddress, byte[] callerAddress,
                                                            long feeLimit, long callValue, BigInteger collateralAmount,
                                                            BigInteger wholeActivatedCollateral) {
        return proc(contractAddress, callerAddress, getMasternodeRewardPerBlock, feeLimit, callValue, collateralAmount,
                wholeActivatedCollateral);
    }

    // getOperatorReward is a free data retrieval call binding the contract method 0xd26f43da.
    //
    // Solidity: function getOperatorReward(uint256 wholeReward, uint256 operatorRewardRatio)
    // constant returns(uint256)
    public TransactionExtention getOperatorReward(byte[] contractAddress, byte[] callerAddress, long feeLimit,
                                                  long callValue, BigInteger wholeReward, BigInteger operatorRewardRatio) {
        return proc(contractAddress, callerAddress, getOperatorReward, feeLimit, callValue, wholeReward,
                operatorRewardRatio);
    }

    // masternodesArray is a free data retrieval call binding the contract method 0x9c915a45.
    //
    // Solidity: function masternodesArray(uint256 ) constant returns(address ownerAuthAddress,
    // address operatorAuthAddress, address ownerRewardAddress, address operatorRewardAddress,
    // uint256 collateralAmount, uint256 operatorRewardRatio, uint256 announcementBlockNumber,
    // uint256 minActivationBlockNumber, uint256 activationBlockNumber, uint256 prevRewardBlockNumber)
    public MasternodesArrayResults masternodesArray(byte[] contractAddress, BigInteger arg0) {
        TransactionExtention results = proc(contractAddress, null, masternodesArray, 0L, 0L, arg0);
        if (results.getConstantResultCount() == 0) {
            return null;
        }

        byte[] blob = results.getConstantResult(0).toByteArray();
        int last = blob.length;

        MasternodesArrayResults result = new MasternodesArrayResults();
        result.OwnerAuthAddress = Arrays.copyOfRange(blob, 0, 32);
        result.OperatorAuthAddress = Arrays.copyOfRange(blob, 32, 64);
        result.OwnerRewardAddress = Arrays.copyOfRange(blob, 64, 96);
        result.OperatorRewardAddress = Arrays.copyOfRange(blob, 96, 128);
        result.CollateralAmount = new BigInteger(Arrays.copyOfRange(blob, 128, 160));
        result.OperatorRewardRatio = new BigInteger(Arrays.copyOfRange(blob, 160, 192));
        result.AnnouncementBlockNumber = new BigInteger(Arrays.copyOfRange(blob, 192, 224));
        result.MinActivationBlockNumber = new BigInteger(Arrays.copyOfRange(blob, 224, 256));
        result.ActivationBlockNumber = new BigInteger(Arrays.copyOfRange(blob, 256, 288));
        result.PrevRewardBlockNumber = new BigInteger(Arrays.copyOfRange(blob, 288, last));

        return result;
    }

    // masternodesRewardsPerBlock is a free data retrieval call binding the contract method 0xcb0c049b.
    //
    // Solidity: function masternodesRewardsPerBlock() constant returns(uint256)
    public TransactionExtention masternodesRewardsPerBlock(byte[] contractAddress, byte[] callerAddress,
                                                           long feeLimit, long callValue) {
        return proc(contractAddress, callerAddress, masternodesRewardsPerBlock, feeLimit, callValue);
    }

    // maxRewardHistoryHops is a free data retrieval call binding the contract method 0x7ada15d2.
    //
    // Solidity: function maxRewardHistoryHops() constant returns(uint256)
    public TransactionExtention maxRewardHistoryHops(byte[] contractAddress, byte[] callerAddress,
                                                     long feeLimit, long callValue) {
        return proc(contractAddress, callerAddress, maxRewardHistoryHops, feeLimit, callValue);
    }

    // maxRewardRatio is a free data retrieval call binding the contract method 0x7cca5429.
    //
    // Solidity: function maxRewardRatio() constant returns(uint256)
    public TransactionExtention maxRewardRatio(byte[] contractAddress, byte[] callerAddress, long feeLimit,
                                               long callValue) {
        return proc(contractAddress, callerAddress, maxRewardRatio, feeLimit, callValue);
    }

    // minBlocksBeforeMnActivation is a free data retrieval call binding the contract method 0x9e62970b.
    //
    // Solidity: function minBlocksBeforeMnActivation() constant returns(uint256)
    public TransactionExtention minBlocksBeforeMnActivation(byte[] contractAddress) {
        return proc(contractAddress, null, minBlocksBeforeMnActivation, 0L, 0L);
    }

    // mnOperatorIndexes is a free data retrieval call binding the contract method 0x971c7b5f.
    //
    // Solidity: function mnOperatorIndexes(address ) constant returns(uint256)
    public TransactionExtention mnOperatorIndexes(byte[] contractAddress, String arg0) {
        return proc(contractAddress, null, mnOperatorIndexes, 0L, 0L, arg0);
    }

    // mnOwnerIndexes is a free data retrieval call binding the contract method 0x171a2ec9.
    //
    // Solidity: function mnOwnerIndexes(address ) constant returns(uint256)
    public TransactionExtention mnOwnerIndexes(byte[] contractAddress, String arg0) {
        return proc(contractAddress, null, mnOwnerIndexes, 0L, 0L, arg0);
    }

    // mnsHistory is a free data retrieval call binding the contract method 0xa302a6c5.
    //
    // Solidity: function mnsHistory(uint256 ) constant returns(uint256 blockNumber, uint256 numOfActivatedMasternodes,
    // uint256 wholeActivatedCollateral, uint256 rewardsPerBlock)
    public MnsHistoryResults mnsHistory(byte[] contractAddress, BigInteger arg0) {
        TransactionExtention results = proc(contractAddress, null, mnsHistory, 0L, 0L, arg0);

        if (results.getConstantResultCount() == 0) {
            return null;
        }

        byte[] blob = results.getConstantResult(0).toByteArray();
        int last = blob.length;

        MnsHistoryResults result = new MnsHistoryResults();
        result.BlockNumber = new BigInteger(Arrays.copyOfRange(blob, 0, 32));
        result.NumOfActivatedMasternodes = new BigInteger(Arrays.copyOfRange(blob, 32, 64));
        result.WholeActivatedCollateral = new BigInteger(Arrays.copyOfRange(blob, 64, 96));
        result.RewardsPerBlock = new BigInteger(Arrays.copyOfRange(blob, 96, last));

        return result;
    }

    // numOfActivatedMasternodes is a free data retrieval call binding the contract method 0xcb7fe143.
    //
    // Solidity: function numOfActivatedMasternodes() constant returns(uint256)
    public TransactionExtention numOfActivatedMasternodes(byte[] contractAddress) {
        return proc(contractAddress, null, numOfActivatedMasternodes, 0L, 0L);
    }

    // wholeActivatedCollateral is a free data retrieval call binding the contract method 0x79d0b421.
    //
    // Solidity: function wholeActivatedCollateral() constant returns(uint256)
    public TransactionExtention wholeActivatedCollateral(byte[] contractAddress) {
        return proc(contractAddress, null, wholeActivatedCollateral, 0L, 0L);
    }

    // wholeCollateral is a free data retrieval call binding the contract method 0xf87b11b3.
    //
    // Solidity: function wholeCollateral() constant returns(uint256)
    public TransactionExtention wholeCollateral(byte[] contractAddress) {
        return proc(contractAddress, null, wholeCollateral, 0L, 0L);
    }

    // activateMasternode is a paid mutator transaction binding the contract method 0x090d0b34.
    //
    // Solidity: function activateMasternode() returns()
    public TransactionExtention activateMasternode(byte[] contractAddress, byte[] callerAddress, long feeLimit) {
        return proc(contractAddress, callerAddress, activateMasternode, feeLimit, 0L);
    }

    // claimMasternodeReward is a paid mutator transaction binding the contract method 0x04cdc542.
    //
    // Solidity: function claimMasternodeReward() returns()
    public TransactionExtention claimMasternodeReward(byte[] contractAddress, byte[] callerAddress, long feeLimit) {
        return proc(contractAddress, callerAddress, claimMasternodeReward, feeLimit, 0L);
    }

    // resign is a paid mutator transaction binding the contract method 0x69652fcf.
    //
    // Solidity: function resign() returns()
    public TransactionExtention resign(byte[] contractAddress, byte[] callerAddress, long feeLimit) {
        return proc(contractAddress, callerAddress, resign, feeLimit, 0L);
    }

    // setOperator is a paid mutator transaction binding the contract method 0xc88cb026.
    //
    // Solidity: function setOperator(address operator, address operatorRewardAddress,
    // uint256 operatorRewardRatio) returns()
    public TransactionExtention setOperator(byte[] contractAddress, byte[] callerAddress, long feeLimit,
                                            String operator, String operatorRewardAddress,
                                            BigInteger operatorRewardRatio) {
        return proc(contractAddress, callerAddress, setOperator, feeLimit, 0L, operator,
                operatorRewardAddress, operatorRewardRatio);
    }

    // currentRewardsPerBlock is a free data retrieval call binding the contract method 0x23d256ef.
    //
    // Solidity: function currentRewardsPerBlock() constant returns(uint256)
    public TransactionExtention currentRewardsPerBlock(byte[] contractAddress) {
        return proc(contractAddress, null, currentRewardsPerBlock, 0L, 0L);
    }

    public interface Triggerer {
        Protocol.Transaction triggerContract(Contract.TriggerSmartContract triggerSmartContract, TransactionCapsule trxCap, TransactionExtention.Builder builder,
                                             GrpcAPI.Return.Builder retBuilder) throws HeaderNotFound, ContractValidateException, VMIllegalException, ContractExeException;
    }

    // GetMasternodeRewardInfoResults is the output of a call to getMasternodeRewardInfo.
    public class GetMasternodeRewardInfoResults {
        public BigInteger OperatorReward;
        public BigInteger OwnerReward;
    }

    // MasternodesArrayResults is the output of a call to masternodesArray.
    public class MasternodesArrayResults {

        public byte[] OwnerAuthAddress;
        public byte[] OperatorAuthAddress;
        public byte[] OwnerRewardAddress;
        public byte[] OperatorRewardAddress;
        public BigInteger CollateralAmount;
        public BigInteger OperatorRewardRatio;
        public BigInteger AnnouncementBlockNumber;
        public BigInteger MinActivationBlockNumber;
        public BigInteger ActivationBlockNumber;
        public BigInteger PrevRewardBlockNumber;

        public MasternodesArrayResults() {
            this.OwnerAuthAddress = new DataWord(0).getData();
            this.OperatorAuthAddress = new DataWord(0).getData();
            this.OwnerRewardAddress = new DataWord(0).getData();
            this.OperatorRewardAddress = new DataWord(0).getData();
            this.CollateralAmount = BigInteger.valueOf(0L);
            this.OperatorRewardRatio = BigInteger.valueOf(0L);
            this.AnnouncementBlockNumber = BigInteger.valueOf(0L);
            this.MinActivationBlockNumber = BigInteger.valueOf(0L);
            this.ActivationBlockNumber = BigInteger.valueOf(0L);
            this.PrevRewardBlockNumber = BigInteger.valueOf(0L);
        }

        public boolean equals(Object obj) {
            MasternodesArrayResults o = (MasternodesArrayResults) obj;
            if (!Arrays.equals(this.OwnerAuthAddress, o.OwnerAuthAddress)) {
                return false;
            }

            if (!Arrays.equals(this.OperatorAuthAddress, o.OperatorAuthAddress)) {
                return false;
            }

            if (!Arrays.equals(this.OwnerRewardAddress, o.OwnerRewardAddress)) {
                return false;
            }

            if (!Arrays.equals(this.OperatorRewardAddress, o.OperatorRewardAddress)) {
                return false;
            }

            if (!this.CollateralAmount.equals(o.CollateralAmount)) {
                return false;
            }

            if (!this.OperatorRewardRatio.equals(o.OperatorRewardRatio)) {
                return false;
            }

            if (!this.AnnouncementBlockNumber.equals(o.AnnouncementBlockNumber)) {
                return false;
            }

            if (!this.MinActivationBlockNumber.equals(o.MinActivationBlockNumber)) {
                return false;
            }

            if (!this.ActivationBlockNumber.equals(o.ActivationBlockNumber)) {
                return false;
            }

            return this.PrevRewardBlockNumber.equals(o.PrevRewardBlockNumber);
        }
    }

    // MnsHistoryResults is the output of a call to mnsHistory.
    public class MnsHistoryResults {
        public BigInteger BlockNumber;
        public BigInteger NumOfActivatedMasternodes;
        public BigInteger WholeActivatedCollateral;
        public BigInteger RewardsPerBlock;

        public boolean equals(Object obj) {
            MnsHistoryResults o = (MnsHistoryResults) obj;
            if (!this.BlockNumber.equals(o.BlockNumber)) {
                return false;
            }

            if (!this.NumOfActivatedMasternodes.equals(o.NumOfActivatedMasternodes)) {
                return false;
            }

            if (!this.WholeActivatedCollateral.equals(o.WholeActivatedCollateral)) {
                return false;
            }

            return this.RewardsPerBlock.equals(o.RewardsPerBlock);
        }
    }
}