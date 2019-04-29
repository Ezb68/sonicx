package org.sonicx.core.db2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.application.Application;
import org.sonicx.common.application.ApplicationFactory;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.common.utils.SessionOptional;
import org.sonicx.core.Constant;
import org.sonicx.core.capsule.ProtoCapsule;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingSonicxStore;
import org.sonicx.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.sonicx.core.db2.core.ISession;
import org.sonicx.core.db2.core.Snapshot;
import org.sonicx.core.db2.core.SnapshotManager;
import org.sonicx.core.db2.core.SnapshotRoot;

public class SnapshotRootTest {

  private TestRevokingSonicxStore sonicxDatabase;
  private SonicxApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"}, Constant.TEST_CONF);
    context = new SonicxApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testRemove() {
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("test".getBytes());
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotRoot-testRemove");
    sonicxDatabase.put("test".getBytes(), testProtoCapsule);
    Assert.assertEquals(testProtoCapsule, sonicxDatabase.get("test".getBytes()));

    sonicxDatabase.delete("test".getBytes());
    Assert.assertEquals(null, sonicxDatabase.get("test".getBytes()));
    sonicxDatabase.close();
  }

  @Test
  public synchronized void testMerge() {
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotRoot-testMerge");
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    revokingDatabase.add(sonicxDatabase.getRevokingDB());

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("merge".getBytes());
    sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.getDbs().forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    dialog.reset();
    Assert.assertEquals(sonicxDatabase.get(testProtoCapsule.getData()), testProtoCapsule);

    sonicxDatabase.close();
  }

  @Test
  public synchronized void testMergeList() {
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotRoot-testMergeList");
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    revokingDatabase.add(sonicxDatabase.getRevokingDB());

    SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("test".getBytes());
    sonicxDatabase.put("merge".getBytes(), testProtoCapsule);
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest tmpProtoCapsule = new ProtoCapsuleTest(("mergeList" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(tmpProtoCapsule.getData(), tmpProtoCapsule);
        tmpSession.commit();
      }
    }
    revokingDatabase.getDbs().forEach(db -> {
      List<Snapshot> snapshots = new ArrayList<>();
      SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
      Snapshot next = root;
      for (int i = 0; i < 11; ++i) {
        next = next.getNext();
        snapshots.add(next);
      }
      root.merge(snapshots);
      root.resetSolidity();

      for (int i = 1; i < 11; i++) {
        ProtoCapsuleTest tmpProtoCapsule = new ProtoCapsuleTest(("mergeList" + i).getBytes());
        Assert.assertEquals(tmpProtoCapsule, sonicxDatabase.get(tmpProtoCapsule.getData()));
      }

    });
    revokingDatabase.updateSolidity(10);
    sonicxDatabase.close();
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class ProtoCapsuleTest implements ProtoCapsule<Object> {

    private byte[] value;

    @Override
    public byte[] getData() {
      return value;
    }

    @Override
    public Object getInstance() {
      return value;
    }

    @Override
    public String toString() {
      return "ProtoCapsuleTest{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }
}
