package org.sonicx.core.db2;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.SonicxStoreWithRevoking;
import org.sonicx.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.sonicx.core.db2.core.ISession;
import org.sonicx.core.db2.core.SnapshotManager;
import org.sonicx.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingDbWithCacheNewValueTest {

  private SnapshotManager revokingDatabase;
  private SonicxApplicationContext context;
  private Application appT;
  private TestRevokingSonicxStore sonicxDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
    context = new SonicxApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    sonicxDatabase.close();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testRevokingDBWithCacheNewValue-testPop");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(1, revokingDatabase.getActiveSession());
        tmpSession.commit();
        Assert.assertEquals(i, revokingDatabase.getSize());
        Assert.assertEquals(0, revokingDatabase.getActiveSession());
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getSize());
    }

    Assert.assertEquals(0, revokingDatabase.getSize());
  }

  @Test
  public synchronized void testMerge() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testRevokingDBWithCacheNewValue-testMerge");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }
    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    dialog.setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("merge".getBytes());
    ProtoCapsuleTest testProtoCapsule2 = new ProtoCapsuleTest("merge2".getBytes());

    sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule2);
      tmpSession.merge();
    }
    Assert.assertEquals(testProtoCapsule2, sonicxDatabase.get(testProtoCapsule.getData()));

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.delete(testProtoCapsule.getData());
      tmpSession.merge();
    }
    Assert.assertEquals(null, sonicxDatabase.get(testProtoCapsule.getData()));
    dialog.reset();
  }


  @Test
  public synchronized void testRevoke() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testRevokingDBWithCacheNewValue-testRevoke");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());

    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }
    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    for (int i = 0; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(2, revokingDatabase.getSize());
        tmpSession.merge();
        Assert.assertEquals(1, revokingDatabase.getSize());
      }
    }

    Assert.assertEquals(1, revokingDatabase.getSize());
    dialog.reset();
    Assert.assertTrue(revokingDatabase.getSize() == 0);
    Assert.assertEquals(0, revokingDatabase.getActiveSession());

    dialog.setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("revoke".getBytes());
    ProtoCapsuleTest testProtoCapsule2 = new ProtoCapsuleTest("revoke2".getBytes());
    sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    dialog.setValue(revokingDatabase.buildSession());

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule2);
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.put(testProtoCapsule.getData(), new ProtoCapsuleTest("revoke22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.put(testProtoCapsule.getData(), new ProtoCapsuleTest("revoke222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      sonicxDatabase.delete(testProtoCapsule.getData());
      tmpSession.merge();
    }

    dialog.reset();

    logger.info("**********testProtoCapsule:" + String
        .valueOf(sonicxDatabase.getUnchecked(testProtoCapsule.getData())));
    Assert.assertEquals(testProtoCapsule, sonicxDatabase.get(testProtoCapsule.getData()));
  }

  @Test
  public synchronized void testGetlatestValues() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotManager-testGetlatestValues");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("getLastestValues" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    Set<ProtoCapsuleTest> result = sonicxDatabase.getRevokingDB().getlatestValues(5).stream()
        .map(ProtoCapsuleTest::new)
        .collect(Collectors.toSet());

    for (int i = 9; i >= 5; i--) {
      Assert.assertEquals(true,
          result.contains(new ProtoCapsuleTest(("getLastestValues" + i).getBytes())));
    }
  }

  @Test
  public synchronized void testGetValuesNext() {
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotManager-testGetValuesNext");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    for (int i = 1; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("getValuesNext" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    Set<ProtoCapsuleTest> result =
        sonicxDatabase.getRevokingDB().getValuesNext(
            new ProtoCapsuleTest("getValuesNext2".getBytes()).getData(), 3
        ).stream().map(ProtoCapsuleTest::new).collect(Collectors.toSet());

    for (int i = 2; i < 5; i++) {
      Assert.assertEquals(true,
          result.contains(new ProtoCapsuleTest(("getValuesNext" + i).getBytes())));
    }
  }


  public static class TestRevokingSonicxStore extends SonicxStoreWithRevoking<ProtoCapsuleTest> {

    protected TestRevokingSonicxStore(String dbName) {
		super(dbName);
    }

    @Override
    public ProtoCapsuleTest get(byte[] key) {
      byte[] value = this.revokingDB.getUnchecked(key);
      return ArrayUtils.isEmpty(value) ? null : new ProtoCapsuleTest(value);
    }
  }

  public static class TestSnapshotManager extends SnapshotManager {

  }
}
