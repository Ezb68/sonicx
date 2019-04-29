package org.sonicx.core.net.messagehandler;

import org.sonicx.core.exception.P2pException;
import org.sonicx.core.net.message.SonicxMessage;
import org.sonicx.core.net.peer.PeerConnection;

public interface SonicxMsgHandler {

  void processMessage(PeerConnection peer, SonicxMessage msg) throws P2pException;

}
