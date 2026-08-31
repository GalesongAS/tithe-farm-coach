package com.galesong.tithecoach;

import com.google.inject.Inject;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

@PluginDescriptor(
    name = "Tithe Farm Coach (Debug)",
    description = "Development build of Tithe Farm Coach with local run logging",
    tags = {"tithe", "farm", "debug"})
public class TitheFarmCoachDebugPlugin extends Plugin {
  @Inject private PluginManager pluginManager;

  private DebugRunLogger runLogger;

  @Override
  protected void startUp() {
    TitheFarmCoachPlugin coach =
        pluginManager.getPlugins().stream()
            .filter(TitheFarmCoachPlugin.class::isInstance)
            .map(TitheFarmCoachPlugin.class::cast)
            .findFirst()
            .orElse(null);
    if (coach != null) {
      runLogger = new DebugRunLogger(coach);
    }
  }

  @Override
  protected void shutDown() {
    if (runLogger != null) {
      runLogger.close();
      runLogger = null;
    }
  }

  @Subscribe
  public void onGameTick(GameTick event) {
    if (runLogger != null) {
      runLogger.tick();
    }
  }
}
