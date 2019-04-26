package org.sonicx.common.net.udp.message.backup;

import static org.sonicx.common.net.udp.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import org.sonicx.common.net.udp.message.Message;
import org.sonicx.common.overlay.discover.node.Node;
import org.sonicx.protos.Discover;

public class KeepAliveMessage extends Message {

  private Discover.BackupMessage backupMessage;

  public KeepAliveMessage(byte[] data) throws Exception {
    super(BACKUP_KEEP_ALIVE, data);
    backupMessage = Discover.BackupMessage.parseFrom(data);
  }

  public KeepAliveMessage(boolean flag, int priority) {
    super(BACKUP_KEEP_ALIVE, null);
    backupMessage = Discover.BackupMessage.newBuilder().setFlag(flag).setPriority(priority).build();
    data = backupMessage.toByteArray();
  }

  public boolean getFlag() {
    return backupMessage.getFlag();
  }

  public int getPriority() {
    return backupMessage.getPriority();
  }

  @Override
  public Node getFrom() {
    return null;
  }
}
