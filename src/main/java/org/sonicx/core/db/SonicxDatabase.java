package org.sonicx.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonicx.common.storage.leveldb.LevelDbDataSourceImpl;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.api.IndexHelper;
import org.sonicx.core.db2.core.ISonicxChainBase;
import org.sonicx.core.exception.BadItemException;
import org.sonicx.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
public abstract class SonicxDatabase<T> implements ISonicxChainBase<T> {

  protected LevelDbDataSourceImpl dbSource;
  @Getter
  private String dbName;

  @Autowired(required = false)
  protected IndexHelper indexHelper;

  protected SonicxDatabase(String dbName) {
    this.dbName = dbName;
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName(dbName), dbName);
    dbSource.initDB();
  }

  protected SonicxDatabase() {
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  public T getUnchecked(byte[] key) {
    return null;
  }

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
