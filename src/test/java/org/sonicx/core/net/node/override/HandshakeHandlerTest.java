package org.sonicx.core.net.node.override;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.sonicx.common.overlay.discover.node.Node;
import org.sonicx.common.overlay.message.HelloMessage;
import org.sonicx.common.overlay.server.HandshakeHandler;

@Slf4j(topic = "test")
@Component
@Scope("prototype")
public class HandshakeHandlerTest extends HandshakeHandler {

  private Node node;

  public HandshakeHandlerTest() {
  }

  public HandshakeHandlerTest setNode(Node node) {
    this.node = node;
    return this;
  }

  @Override
  protected void sendHelloMsg(ChannelHandlerContext ctx, long time) {
    HelloMessage message = new HelloMessage(node, time,
        manager.getGenesisBlockId(), manager.getSolidBlockId(), manager.getHeadBlockId());
    ctx.writeAndFlush(message.getSendData());
    channel.getNodeStatistics().messageStatistics.addTcpOutMessage(message);
    logger.info("Handshake Send to {}, {}", ctx.channel().remoteAddress(), message);
  }

  public void close() {
    manager.closeAllStore();
  }
}
