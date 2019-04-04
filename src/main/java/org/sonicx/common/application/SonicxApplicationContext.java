package org.sonicx.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.sonicx.common.logsfilter.EventPluginLoader;
import org.sonicx.common.overlay.discover.DiscoverServer;
import org.sonicx.common.overlay.discover.node.NodeManager;
import org.sonicx.common.overlay.server.ChannelManager;
import org.sonicx.core.db.Manager;

public class SonicxApplicationContext extends AnnotationConfigApplicationContext {

  public SonicxApplicationContext() {
  }

  public SonicxApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public SonicxApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public SonicxApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRepushThread();
    dbManager.stopRepushTriggerThread();
    super.destroy();
  }
}
