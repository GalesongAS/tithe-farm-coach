package com.datbear.tithecoach;

import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "Tithe Farm Coach (Debug)",
    description = "Development build of Tithe Farm Coach with local run logging",
    tags = {"tithe", "farm", "debug"})
public class TitheFarmCoachDebugPlugin extends TitheFarmCoachPlugin {
  private DebugRunLogger runLogger;

  @Override
  protected void startUp() {
    super.startUp();
    runLogger = new DebugRunLogger(this);
  }

  @Override
  protected void shutDown() {
    if (runLogger != null) {
      runLogger.close();
      runLogger = null;
    }
    super.shutDown();
  }

  @Override
  @Subscribe
  public void onGameTick(GameTick event) {
    super.onGameTick(event);
    if (runLogger != null) {
      runLogger.tick();
    }
  }
}
