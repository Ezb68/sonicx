/*
 * SonicX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SonicX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sonicx.core.capsule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import lombok.extern.slf4j.Slf4j;
import org.sonicx.common.crypto.Hash;
import org.sonicx.common.logsfilter.ContractEventParserAbi;
import org.sonicx.common.logsfilter.ContractParser;
import org.sonicx.common.runtime.vm.LogInfoTriggerParser;
import org.sonicx.core.Constant;
import org.sonicx.protos.Contract.CreateSmartContract;
import org.sonicx.protos.Contract.TriggerSmartContract;
import org.sonicx.protos.Protocol.SmartContract;
import org.sonicx.protos.Protocol.SmartContract.ABI;
import org.sonicx.protos.Protocol.Transaction;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Slf4j(topic = "capsule")
public class TriggerSmartContractCapsule implements ProtoCapsule<TriggerSmartContract> {

  private SmartContract smartContract;
  private TriggerSmartContract triggerSmartContract;

  /**
   * constructor TransactionCapsule.
   */
  public TriggerSmartContractCapsule(SmartContract smartContract, TriggerSmartContract triggerSmartContract) {
    this.smartContract = smartContract;
    this.triggerSmartContract = triggerSmartContract;
    parseData();
  }

  public void parseData() {
      ABI.Entry entry = findEntry();
      if (entry == null) return;
      byte[] data = this.triggerSmartContract.getData().toByteArray();
      Map<String, String> variablesMap = ContractParser.parseData(data, entry);
      try {
          this.triggerSmartContract = this.triggerSmartContract.toBuilder()
                  .setFunctionName(entry.getName())
                  .setFunctionSignature(LogInfoTriggerParser.getEntrySignature(entry))
                  .setFunctionSignatureFull(LogInfoTriggerParser.getEntrySignatureFull(entry))
                  .putAllVariables(variablesMap).build();
      } catch (Exception ex) {
          logger.error(ex.getMessage());
      }
  }

  public byte[] getCodeHash() {
    return this.smartContract.getCodeHash().toByteArray();
  }

  public void setCodeHash(byte[] codeHash) {
    this.smartContract = this.smartContract.toBuilder().setCodeHash(ByteString.copyFrom(codeHash))
        .build();
  }

  @Override
  public byte[] getData() {
    return this.triggerSmartContract.toByteArray();
  }

  @Override
  public TriggerSmartContract getInstance() {
    return this.triggerSmartContract;
  }

  @Override
  public String toString() {
    return this.triggerSmartContract.toString();
  }

  public ABI getABI() {
    return this.smartContract.getAbi();
  }


  public byte[] getTrxHash() {
    return this.smartContract.getTrxHash().toByteArray();
  }

  public ABI.Entry findEntry() {
    ABI abi = getABI();
    ABI.Entry entry = null;

    byte[] entrySignature = triggerSmartContract.getData().substring(0, 4).toByteArray();
    for (ABI.Entry e : abi.getEntrysList()) {
      if (Arrays.equals(entrySignature,Arrays.copyOfRange(Hash.sha3(LogInfoTriggerParser.getEntrySignature(e).getBytes()), 0, 4))) {
        entry = e;
        return entry;
      }
    }
    return entry;
  }

}
