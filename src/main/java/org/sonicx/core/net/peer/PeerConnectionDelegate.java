package org.sonicx.core.net.peer;

import org.sonicx.common.overlay.message.Message;
import org.sonicx.common.utils.Sha256Hash;
import org.sonicx.core.net.message.SonicxMessage;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, SonicxMessage msg) throws Exception;

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void onConnectPeer(PeerConnection peer);

  public abstract void onDisconnectPeer(PeerConnection peer);

}
