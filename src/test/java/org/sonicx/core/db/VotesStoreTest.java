package org.sonicx.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.core.Constant;
import org.sonicx.core.capsule.VotesCapsule;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.protos.Protocol.Vote;

@Slf4j
public class VotesStoreTest {

  private static final String dbPath = "output-votesStore-test";
  private static SonicxApplicationContext context;
  VotesStore votesStore;

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new SonicxApplicationContext(DefaultConfig.class);
  }

  @Before
  public void initDb() {
    this.votesStore = context.getBean(VotesStore.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void putAndGetVotes() {
    List<Vote> oldVotes = new ArrayList<Vote>();

    VotesCapsule votesCapsule = new VotesCapsule(ByteString.copyFromUtf8("100000000x"), oldVotes);
    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);

    Assert.assertTrue("votesStore is empyt", votesStore.iterator().hasNext());
    Assert.assertTrue(votesStore.has(votesCapsule.createDbKey()));
    VotesCapsule votesSource = this.votesStore
        .get(ByteString.copyFromUtf8("100000000x").toByteArray());
    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
    Assert.assertEquals(ByteString.copyFromUtf8("100000000x"), votesSource.getAddress());

//    votesCapsule = new VotesCapsule(ByteString.copyFromUtf8(""), oldVotes);
//    this.votesStore.put(votesCapsule.createDbKey(), votesCapsule);
//    votesSource = this.votesStore.get(ByteString.copyFromUtf8("").toByteArray());
//    Assert.assertEquals(votesStore.getAllVotes().size(), 2);
//    Assert.assertEquals(votesCapsule.getAddress(), votesSource.getAddress());
//    Assert.assertEquals(null, votesSource.getAddress());
  }
}