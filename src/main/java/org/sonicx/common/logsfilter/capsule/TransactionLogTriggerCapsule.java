package org.sonicx.common.logsfilter.capsule;

import static org.sonicx.protos.Protocol.Transaction.Contract.ContractType.TransferAssetContract;
import static org.sonicx.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.sonicx.common.logsfilter.EventPluginLoader;
import org.sonicx.common.logsfilter.trigger.InternalTransactionPojo;
import org.sonicx.common.logsfilter.trigger.TransactionLogTrigger;
import org.sonicx.common.runtime.vm.program.InternalTransaction;
import org.sonicx.common.runtime.vm.program.ProgramResult;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.db.TransactionTrace;
import org.sonicx.protos.Contract.TransferAssetContract;
import org.sonicx.protos.Contract.TransferContract;
import org.sonicx.protos.Protocol;

@Slf4j
public class TransactionLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  TransactionLogTrigger transactionLogTrigger;

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    transactionLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule) {
    transactionLogTrigger = new TransactionLogTrigger();
    if (Objects.nonNull(blockCapsule)) {
      transactionLogTrigger.setBlockHash(blockCapsule.getBlockId().toString());
    }
    transactionLogTrigger.setTransactionId(trxCapsule.getTransactionId().toString());
    transactionLogTrigger.setTimeStamp(blockCapsule.getTimeStamp());
    transactionLogTrigger.setBlockNumber(trxCapsule.getBlockNum());

    TransactionTrace trxTrace = trxCapsule.getTrxTrace();

    //result
    if (Objects.nonNull(trxCapsule.getContractRet())){
      transactionLogTrigger.setResult(trxCapsule.getContractRet().toString());
    }

    if (Objects.nonNull(trxCapsule.getInstance().getRawData())){
      // feelimit
      transactionLogTrigger.setFeeLimit(trxCapsule.getInstance().getRawData().getFeeLimit());

      Protocol.Transaction.Contract contract = trxCapsule.getInstance().getRawData().getContract(0);
      Any contractParameter = null;
      // contract type
      if (Objects.nonNull(contract)){
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)){
          transactionLogTrigger.setContractType(contractType.toString());
        }

        contractParameter = contract.getParameter();

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
        try{
          if (contract.getType() == TransferContract) {
              TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);

              if (Objects.nonNull(contractTransfer)){
                transactionLogTrigger.setAssetName("sox");

                if (Objects.nonNull(contractTransfer.getOwnerAddress())){
                  transactionLogTrigger.setFromAddress(Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                }

                if (Objects.nonNull(contractTransfer.getToAddress())){
                  transactionLogTrigger.setToAddress(Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
                }

                transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
              }

          } else if (contract.getType() == TransferAssetContract) {
            TransferAssetContract contractTransfer = contractParameter.unpack(TransferAssetContract.class);

            if (Objects.nonNull(contractTransfer)){
              if (Objects.nonNull(contractTransfer.getAssetName())){
                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
              }

              if (Objects.nonNull(contractTransfer.getOwnerAddress())){
                transactionLogTrigger.setFromAddress(Wallet.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())){
                transactionLogTrigger.setToAddress(Wallet.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }
              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
          }
        }
        catch (Exception e) {
          logger.error("failed to load transferAssetContract, error'{}'", e);
        }
      }
    }

    // receipt
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
      transactionLogTrigger.setEnergyFee(trxTrace.getReceipt().getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(trxTrace.getReceipt().getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(trxTrace.getReceipt().getEnergyUsageTotal());
      transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
      transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
      transactionLogTrigger.setEnergyUsage(trxTrace.getReceipt().getEnergyUsage());
    }

    // program result
    ProgramResult programResult = trxTrace.getRuntime().getResult();
    if (Objects.nonNull(trxTrace) && Objects.nonNull(programResult)){
      ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
      ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

      if (Objects.nonNull(contractResult) && contractResult.size() > 0){
        transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0){
        transactionLogTrigger.setContractAddress(Wallet.encode58Check((contractAddress.toByteArray())));
      }

      // internal transaction
      transactionLogTrigger.setInternalTrananctionList(getInternalTransactionList(programResult.getInternalTransactions()));
    }
  }

  private List<InternalTransactionPojo> getInternalTransactionList(List<InternalTransaction> internalTransactionList){
    List<InternalTransactionPojo> pojoList = new ArrayList<>();

    internalTransactionList.forEach(internalTransaction -> {
      InternalTransactionPojo item = new InternalTransactionPojo();

      item.setHash(Hex.toHexString(internalTransaction.getHash()));
      item.setCallValue(internalTransaction.getValue());
      item.setTokenInfo(internalTransaction.getTokenInfo());
      item.setCaller_address(Hex.toHexString(internalTransaction.getSender()));
      item.setTransferTo_address(Hex.toHexString(internalTransaction.getTransferToAddress()));
      item.setData(Hex.toHexString(internalTransaction.getData()));
      item.setRejected(internalTransaction.isRejected());
      item.setNote(internalTransaction.getNote());

      pojoList.add(item);
    });

    return pojoList;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
