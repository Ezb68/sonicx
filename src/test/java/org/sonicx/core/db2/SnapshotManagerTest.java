package org.sonicx.core.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.application.Application;
import org.sonicx.common.application.ApplicationFactory;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.core.Constant;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.CheckTmpStore;
import org.sonicx.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingSonicxStore;
import org.sonicx.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.sonicx.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.sonicx.core.db2.core.ISession;
import org.sonicx.core.db2.core.SnapshotManager;
import org.sonicx.core.exception.BadItemException;
import org.sonicx.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

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
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    sonicxDatabase = new TestRevokingSonicxStore("testSnapshotManager-test");
    revokingDatabase.add(sonicxDatabase.getRevokingDB());
    revokingDatabase.setCheckTmpStore(context.getBean(CheckTmpStore.class));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    sonicxDatabase.close();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
    revokingDatabase.getCheckTmpStore().getDbSource().closeDB();
    sonicxDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        sonicxDatabase.put(protoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new ProtoCapsuleTest("refresh10".getBytes()),
        sonicxDatabase.get(protoCapsule.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("close" + i).getBytes());
      try (ISession _ = revokingDatabase.buildSession()) {
        sonicxDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        sonicxDatabase.get(protoCapsule.getData()));

  }
}
