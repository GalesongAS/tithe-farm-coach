package com.galesong.tithecoach;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;

final class TitheFarmCoachPanel extends OverlayPanel {
  private final TitheFarmCoachPlugin plugin;
  private final TitheFarmCoachConfig config;

  @Inject
  TitheFarmCoachPanel(TitheFarmCoachPlugin plugin, TitheFarmCoachConfig config) {
    this.plugin = plugin;
    this.config = config;
    setPosition(OverlayPosition.TOP_LEFT);
    setPriority(Overlay.PRIORITY_HIGH);
  }

  @Override
  public Dimension render(Graphics2D g) {
    if (!plugin.isInTitheFarm()) {
      return null;
    }

    CoachStep s = plugin.getStep();
    panelComponent.setPreferredSize(new Dimension(310, 0));
    Color title =
        s.kind == CoachStep.Kind.RECOVER
            ? new Color(255, 80, 80)
            : s.kind == CoachStep.Kind.WAIT ? new Color(255, 205, 60) : new Color(100, 255, 130);
    panelComponent.getChildren().add(TitleComponent.builder().text("TITHE FARM COACH").build());
    panelComponent.getChildren().add(TitleComponent.builder().text(s.title).color(title).build());
    if (config.showActionDetails()) {
      for (String line : wrap(s.detail, 43)) {
        addLine(line, "");
      }
    }

    if (config.showRunStatus()) {
      addLine("Method:", config.method().toString());
      if (plugin.getQueuedPlantTarget() != null) {
        addLine(
            "Next planting click:",
            plugin.isQueuedPlantClickReady() ? "READY" : "WAIT",
            plugin.isQueuedPlantClickReady() ? new Color(100, 255, 130) : new Color(255, 80, 80));
      }
      addLine(
          "Fruit harvested:", plugin.getHarvestedTotal() + "/" + CoachEngine.FRUIT_PER_FULL_SET);
      addLine("Route patches:", String.valueOf(plugin.getRoute().size()));
    }

    if (config.showWaterStatus()) {
      int doses = plugin.getWaterDoseCount();
      int needed = plugin.getRemainingWaterNeeded();
      addLine(
          plugin.isUsingGricollersCan() ? "Gricoller charges / needed:" : "Water doses / needed:",
          doses + " / " + needed,
          doses < needed ? new Color(255, 80, 80) : new Color(100, 255, 130));
    }

    if (config.showRewardSummary() || config.showShopBreakdown()) {
      panelComponent.getChildren().add(TitleComponent.builder().text("REWARD PROGRESS").build());
    }
    if (config.showRewardSummary()) {
      rewardSummary();
    }
    if (config.showShopBreakdown()) {
      reward("Farmer boots", 50);
      reward("Auto-weed", 50);
      reward("Farmer strawhat", 75);
      reward("Farmer trousers", 125);
      reward("Farmer top", 150);
      reward("Gricoller's can", 200);
      reward("Seed box", 250);
      reward("Herb sack", 250);
    }
    return super.render(g);
  }

  private void rewardSummary() {
    addLine("Spendable points:", String.valueOf(plugin.getRewardPoints()), new Color(255, 205, 60));
    addLine("Earned this launch:", "+" + plugin.getSessionPointsEarned());
    addLine(
        "Current 100-fruit set:",
        plugin.getCurrentSetPointsEarned() + " / " + CoachEngine.POINTS_PER_FULL_SET + " pts");
    addLine("Batches per set:", String.valueOf(plugin.getBatchesPerFullSet()));
    addLine(
        "Permanent shop:",
        plugin.getPermanentShopProgress() + " / " + CoachEngine.PERMANENT_REWARD_TOTAL);
    addLine(
        "Full sets to buyout:",
        String.valueOf(plugin.getFullSetsToPermanentBuyout()),
        new Color(255, 205, 60));
  }

  private void reward(String name, int cost) {
    int remaining = Math.max(0, cost - plugin.getRewardPoints());
    addLine(
        name + " (" + cost + ")",
        remaining == 0 ? "CAN BUY" : remaining + " more",
        remaining == 0 ? new Color(100, 255, 130) : Color.WHITE);
  }

  private void addLine(String label, String value) {
    addLine(label, value, Color.WHITE);
  }

  private void addLine(String label, String value, Color color) {
    panelComponent
        .getChildren()
        .add(LineComponent.builder().left(label).right(value).rightColor(color).build());
  }

  private String[] wrap(String text, int width) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    StringBuilder line = new StringBuilder();
    for (String word : text.split(" ")) {
      if (line.length() > 0 && line.length() + word.length() + 1 > width) {
        lines.add(line.toString());
        line.setLength(0);
      }
      if (line.length() > 0) line.append(' ');
      line.append(word);
    }
    if (line.length() > 0) lines.add(line.toString());
    return lines.toArray(new String[0]);
  }
}
