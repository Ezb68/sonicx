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

package org.sonicx.core;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.capsule.utils.BlockUtil;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.exception.BadTransactionException;
import org.sonicx.core.exception.ContractValidateException;
import org.sonicx.core.services.http.JsonFormat;
import org.sonicx.protos.Protocol.Block;
import org.sonicx.protos.Protocol.BlockHeader;
import org.sonicx.protos.Protocol.BlockHeader.raw;

@Slf4j
public class BlockUtilTest {

  @Before
  public void initConfiguration() {
    Args.setParam(new String[]{}, Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
  }

  @Test
  public void testBlockUtil() throws BadTransactionException, ContractValidateException, JsonFormat.ParseException {
    //test create GenesisBlockCapsule
    BlockCapsule blockCapsule1 = BlockUtil.newGenesisBlockCapsule();
    Sha256Hash sha256Hash = Sha256Hash.wrap(ByteArray
        .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"));

    Assert.assertEquals(0, blockCapsule1.getTimeStamp());
    Assert.assertEquals(sha256Hash,
        blockCapsule1.getParentHash());
    Assert.assertEquals(0, blockCapsule1.getNum());

    //test isParentOf method: create blockCapsule2 and blockCapsule3
    // blockCapsule3.setParentHash() equals blockCapsule2.getBlockId
    BlockCapsule blockCapsule2 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString("0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81")))
        )).build());

    BlockCapsule blockCapsule3 = new BlockCapsule(Block.newBuilder().setBlockHeader(
        BlockHeader.newBuilder().setRawData(raw.newBuilder().setParentHash(ByteString.copyFrom(
            ByteArray
                .fromHexString(blockCapsule2.getBlockId().toString())))
        )).build());

    Assert.assertEquals(false, BlockUtil.isParentOf(blockCapsule1, blockCapsule2));
    Assert.assertFalse(BlockUtil.isParentOf(blockCapsule1, blockCapsule2));
    Assert.assertEquals(true, BlockUtil.isParentOf(blockCapsule2, blockCapsule3));
    Assert.assertTrue(BlockUtil.isParentOf(blockCapsule2, blockCapsule3));
  }

  @Test
  public void testGenesisBlock() throws BadTransactionException, ContractValidateException, JsonFormat.ParseException {
    //test create GenesisBlockCapsule
    BlockCapsule blockCapsule1 = BlockUtil.newGenesisBlockCapsule();
    Assert.assertEquals(5, blockCapsule1.getTransactions().size());

    for (TransactionCapsule tx : blockCapsule1.getTransactions()) {
      System.out.printf("%s", tx.getInstance());
    }
  }
}
