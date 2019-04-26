package org.sonicx.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.sonicx.common.overlay.server.Channel;
import org.sonicx.common.overlay.server.MessageQueue;
import org.sonicx.core.net.message.SonicxMessage;

@Component
@Scope("prototype")
public class SonicxNetHandler extends SimpleChannelInboundHandler<SonicxMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue = null;

  public PeerConnectionDelegate peerDel;

  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, SonicxMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    peerDel.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}
