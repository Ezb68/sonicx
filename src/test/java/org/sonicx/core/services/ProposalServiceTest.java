package org.sonicx.core.services;

import static org.sonicx.core.services.ProposalService.ProposalType.WITNESS_127_PAY_PER_BLOCK;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.core.Constant;
import org.sonicx.core.capsule.ProposalCapsule;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.Manager;
import org.sonicx.core.services.ProposalService.ProposalType;
import org.sonicx.protos.Protocol.Proposal;

public class ProposalServiceTest {

  private SonicxApplicationContext context;
  private Manager manager;
  private String dbPath = "output_proposal_test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new SonicxApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    manager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(5);
  }

  @Test
  public void test() {
    Set<Long> set = new HashSet<>();
    for (ProposalType proposalType : ProposalType.values()) {
      Assert.assertTrue(set.add(proposalType.getCode()));
    }

    Proposal proposal = Proposal.newBuilder().putParameters(1, 1).build();
    ProposalCapsule proposalCapsule = new ProposalCapsule(proposal);
    boolean result = ProposalService.process(manager, proposalCapsule);
    Assert.assertTrue(result);
    //
    proposal = Proposal.newBuilder().putParameters(1000, 1).build();
    proposalCapsule = new ProposalCapsule(proposal);
    result = ProposalService.process(manager, proposalCapsule);
    Assert.assertFalse(result);
    //
    for (ProposalType proposalType : ProposalType.values()) {
      if (proposalType == WITNESS_127_PAY_PER_BLOCK) {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 16160).build();
      } else {
        proposal = Proposal.newBuilder().putParameters(proposalType.getCode(), 1).build();
      }
      proposalCapsule = new ProposalCapsule(proposal);
      result = ProposalService.process(manager, proposalCapsule);
      Assert.assertTrue(result);
    }
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}
