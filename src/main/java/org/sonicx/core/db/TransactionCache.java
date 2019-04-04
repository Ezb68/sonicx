package org.sonicx.core.db;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sonicx.common.utils.ByteArray;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.capsule.BlockCapsule;
import org.sonicx.core.capsule.BytesCapsule;
import org.sonicx.core.capsule.TransactionCapsule;
import org.sonicx.core.db.KhaosDatabase.KhaosBlock;
import org.sonicx.core.db2.common.TxCacheDB;
import org.sonicx.core.exception.BadItemException;
import org.sonicx.core.exception.StoreException;

@Slf4j
public class TransactionCache extends SonicxStoreWithRevoking<BytesCapsule> {

  @Autowired
  public TransactionCache(@Value("trans-cache") String dbName) {
    super(dbName, TxCacheDB.class);
  }
}
