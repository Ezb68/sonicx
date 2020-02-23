package org.sonicx.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.sonicx.common.overlay.server.Channel;
import org.sonicx.common.overlay.server.MessageQueue;
import org.sonicx.core.net.message.SonicxMessage;
import org.sonicx.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class SonicxNetHandler extends SimpleChannelInboundHandler<SonicxMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private SonicxNetService sonicxNetService;

//  @Autowired
//  private SonicxNetHandler (final ApplicationContext ctx){
//    sonicxNetService = ctx.getBean(SonicxNetService.class);
//  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, SonicxMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    sonicxNetService.onMessage(peer, msg);
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