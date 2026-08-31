package com.datbear.tithecoach;

import net.runelite.client.config.*;

@ConfigGroup("tithefarmcoach")
public interface TitheFarmCoachConfig extends Config {
  String PANEL_SECTION = "panel";

  @ConfigItem(
      keyName = "method",
      name = "Method",
      description =
          "Choose a route from most forgiving to most time-sensitive; larger routes leave less"
              + " recovery time",
      position = 0)
  default TitheMethod method() {
    return TitheMethod.STANDARD_20;
  }

  @ConfigItem(
      keyName = "showRouteNumbers",
      name = "Show route numbers",
      description = "Number every selected patch while highlighting the current one",
      position = 1)
  default boolean showRouteNumbers() {
    return true;
  }

  @ConfigItem(
      keyName = "readableTimers",
      name = "High-contrast plant timers",
      description = "Show a large progress bar and seconds over every live route plant",
      position = 2)
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
      position = 3)
  default int permanentPointsSpent() {
    return 0;
  }

  @ConfigSection(
      name = "Panel contents",
      description = "Choose which information is shown in the coach text panel",
      position = 4)
  String panelSection = PANEL_SECTION;

  @ConfigItem(
      keyName = "showActionDetails",
      name = "Action explanation",
      description = "Show the longer explanation below the current action",
      position = 5,
      section = PANEL_SECTION)
  default boolean showActionDetails() {
    return true;
  }

  @ConfigItem(
      keyName = "showRunStatus",
      name = "Run status",
      description = "Show method, fruit, route size, and queued planting status",
      position = 6,
      section = PANEL_SECTION)
  default boolean showRunStatus() {
    return true;
  }

  @ConfigItem(
      keyName = "showWaterStatus",
      name = "Water status",
      description = "Show available water doses or Gricoller's can charges",
      position = 7,
      section = PANEL_SECTION)
  default boolean showWaterStatus() {
    return true;
  }

  @ConfigItem(
      keyName = "showRewardSummary",
      name = "Reward summary",
      description = "Show points, current set progress, batches, and buyout estimate",
      position = 8,
      section = PANEL_SECTION)
  default boolean showRewardSummary() {
    return true;
  }

  @ConfigItem(
      keyName = "showShopBreakdown",
      name = "Shop breakdown",
      description = "Show the cost and affordability of every permanent reward",
      position = 9,
      section = PANEL_SECTION)
  default boolean showShopBreakdown() {
    return true;
  }

}
