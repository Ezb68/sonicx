package org.sonicx.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.common.utils.StringUtil;
import org.sonicx.core.Wallet;
import org.sonicx.core.capsule.AccountCapsule;
import org.sonicx.core.capsule.TransactionResultCapsule;
import org.sonicx.core.db.Manager;
import org.sonicx.core.db.StorageMarket;
import org.sonicx.core.exception.ContractExeException;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.protos.Contract.SellStorageContract;
import org.sonicx.protos.Protocol.Transaction.Result.code;

@Slf4j(topic = "actuator")
public class SellStorageActuator extends AbstractActuator {

  private StorageMarket storageMarket;

  SellStorageActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    storageMarket = new StorageMarket(dbManager);
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    long fee = calcFee();
    final SellStorageContract sellStorageContract;
    try {
      sellStorageContract = contract.unpack(SellStorageContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(sellStorageContract.getOwnerAddress().toByteArray());

    long bytes = sellStorageContract.getStorageBytes();

    storageMarket.sellStorage(accountCapsule, bytes);

    ret.setStatus(fee, code.SUCESS);

    return true;
  }


  @Override
  public boolean validate() throws ContractValidateException {
    if (this.contract == null) {
      throw new ContractValidateException("No contract!");
    }
    if (this.dbManager == null) {
      throw new ContractValidateException("No dbManager!");
    }
    if (!contract.is(SellStorageContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [SellStorageContract],real type[" + contract
              .getClass() + "]");
    }

    final SellStorageContract sellStorageContract;
    try {
      sellStorageContract = this.contract.unpack(SellStorageContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = sellStorageContract.getOwnerAddress().toByteArray();
    if (!Wallet.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    long bytes = sellStorageContract.getStorageBytes();
    if (bytes <= 0) {
      throw new ContractValidateException("bytes must be positive");
    }

    long currentStorageLimit = accountCapsule.getStorageLimit();
    long currentUnusedStorage = currentStorageLimit - accountCapsule.getStorageUsage();

    if (bytes > currentUnusedStorage) {
      throw new ContractValidateException(
          "bytes must be less than currentUnusedStorage[" + currentUnusedStorage + "]");
    }

    long quantity = storageMarket.trySellStorage(bytes);
    if (quantity <= 1_000_000L) {
      throw new ContractValidateException(
          "quantity must be larger than 1SOX,current quantity[" + quantity + "]");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(SellStorageContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
