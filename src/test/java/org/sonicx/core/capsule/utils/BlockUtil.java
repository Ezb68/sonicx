package org.sonicx.core.capsule.utils;


import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.config.args.GenesisBlock;
import org.sonicx.protos.Protocol.Transaction;

import java.util.Map;
import org.sonicx.core.db.Manager;
import org.sonicx.core.mastrnode.MasterNodeController;
import org.sonicx.core.services.http.JsonFormat;
import org.sonicx.core.witness.WitnessController;
import org.sonicx.common.utils.Sha256Hash;

public class BlockUtil {

  /**
   * create genesis block from transactions.
   */
  public static BlockCapsule newGenesisBlockCapsule() throws JsonFormat.ParseException {

    Args args = Args.getInstance();
    GenesisBlock genesisBlockArg = args.getGenesisBlock();
    List<Transaction> transactionList =
        genesisBlockArg.getAssets().stream()
            .map(key -> {
              byte[] address = key.getAddress();
              long balance = key.getBalance();
              return TransactionUtil.newGenesisTransaction(address, balance);
            })
            .collect(Collectors.toList());

    if (args.getMasternode().isEnable()) {
      ByteString ownerAddress = ByteString.copyFrom("0x000000000000000000000".getBytes());
      long minimumCollateral = args.getMasternode().getMinimumCollateral();

      Transaction mnTx = MasterNodeController.deploy(ownerAddress.toByteArray(), 32000L, minimumCollateral);
      transactionList.add(mnTx);
    }

    long timestamp = Long.parseLong(genesisBlockArg.getTimestamp());
    ByteString parentHash =
        ByteString.copyFrom(ByteArray.fromHexString(genesisBlockArg.getParentHash()));
    long number = Long.parseLong(genesisBlockArg.getNumber());

    BlockCapsule blockCapsule = new BlockCapsule(timestamp, parentHash, number, transactionList);

    blockCapsule.setMerkleRoot();
    blockCapsule.setWitness(
        "A new system must allow existing systems to be linked together without requiring any central control or coordination");
    blockCapsule.generatedByMyself = true;

    return blockCapsule;
  }

  public static boolean isParentOf(BlockCapsule blockCapsule1, BlockCapsule blockCapsule2) {
    return blockCapsule1.getBlockId().equals(blockCapsule2.getParentHash());
  }

  public static BlockCapsule createTestBlockCapsule(Manager dbManager, long time,
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
}