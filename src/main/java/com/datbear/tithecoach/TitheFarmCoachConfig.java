package com.datbear.tithecoach;

import net.runelite.client.config.*;

@ConfigGroup("tithefarmcoach")
public interface TitheFarmCoachConfig extends Config {
  @ConfigItem(
      keyName = "method",
      name = "Method",
      description = "Number of plants managed per batch",
      position = 0)
  default TitheMethod method() {
    return TitheMethod.STANDARD_20;
  }

  @ConfigItem(
      keyName = "voice",
      name = "English voice prompts",
      description = "Speak once when the required action changes",
      position = 1)
  default boolean voice() {
    return false;
  }

  @ConfigItem(
      keyName = "notifications",
      name = "Step notifications",
      description = "Send a RuneLite notification when the step changes",
      position = 2)
  default boolean notifications() {
    return false;
  }

  @ConfigItem(
      keyName = "showRouteNumbers",
      name = "Show route numbers",
      description = "Number every selected patch while highlighting the current one",
      position = 3)
  default boolean showRouteNumbers() {
    return true;
  }

  @ConfigItem(
      keyName = "readableTimers",
      name = "High-contrast plant timers",
      description = "Show a large progress bar and seconds over every live route plant",
      position = 4)
  default boolean readableTimers() {
    return true;
  }

  @Range(min = 0, max = 1150)
  @ConfigItem(
      keyName = "permanentPointsSpent",
      name = "Points spent on permanent rewards",
      description =
          "Enter points already spent on outfit pieces, Auto-weed, Gricoller's can, seed box, or"
              + " herb sack",
      position = 5)
  default int permanentPointsSpent() {
    return 0;
  }
}
