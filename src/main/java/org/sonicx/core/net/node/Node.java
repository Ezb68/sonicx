package org.sonicx.core.net.node;

import org.sonicx.common.overlay.message.Message;
import org.sonicx.common.utils.Quitable;
import org.sonicx.common.utils.Sha256Hash;

public interface Node extends Quitable {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;
}
