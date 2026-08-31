package com.datbear.tithecoach;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TitheFarmCoachPluginTest {
  public static void main(String[] args) throws Exception {
    ExternalPluginManager.loadBuiltin(
        TitheFarmCoachPlugin.class, TitheFarmCoachDebugPlugin.class);
    RuneLite.main(args);
  }
}
