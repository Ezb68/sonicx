package org.sonicx.core.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.sonicx.core.capsule.BytesCapsule;
import org.sonicx.core.db2.common.TxCacheDB;

@Slf4j
public class TransactionCache extends SonicxStoreWithRevoking<BytesCapsule> {

  @Autowired
  public TransactionCache(@Value("trans-cache") String dbName) {
    super(dbName, TxCacheDB.class);
  }
}
