package org.sonicx.core.net;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.sonicx.core.services.NodeInfoServiceTest;

@Slf4j
public class BaseNetTest extends BaseNet {

  @Test
  public void test() throws Exception {
    new NodeInfoServiceTest(context).test();
    new UdpTest(context).test();
    new TcpTest(context).test();
  }
}
