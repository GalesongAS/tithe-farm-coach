package com.datbear.tithecoach;

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
    CoachStep s = plugin.getStep();
    panelComponent.setPreferredSize(new Dimension(310, 0));
    Color title =
        s.kind == CoachStep.Kind.RECOVER
            ? new Color(255, 80, 80)
            : s.kind == CoachStep.Kind.WAIT ? new Color(255, 205, 60) : new Color(100, 255, 130);
    panelComponent.getChildren().add(TitleComponent.builder().text("TITHE FARM COACH").build());
    panelComponent
        .getChildren()
        .add(LineComponent.builder().left("Method:").right(config.method().toString()).build());
    panelComponent.getChildren().add(TitleComponent.builder().text(s.title).color(title).build());
    for (String line : wrap(s.detail, 43))
      panelComponent.getChildren().add(LineComponent.builder().left(line).build());
    if (plugin.getQueuedPlantTarget() != null)
      panelComponent
          .getChildren()
          .add(
              LineComponent.builder()
                  .left("Next planting click:")
                  .right(plugin.isQueuedPlantClickReady() ? "READY" : "WAIT")
                  .rightColor(
                      plugin.isQueuedPlantClickReady()
                          ? new Color(100, 255, 130)
                          : new Color(255, 80, 80))
                  .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Fruit harvested:")
                .right(plugin.getHarvestedTotal() + "/100")
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Route patches:")
                .right(String.valueOf(plugin.getRoute().size()))
                .build());
    int doses = plugin.getWaterDoseCount(), needed = plugin.getRemainingWaterNeeded();
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left(
                    plugin.isUsingGricollersCan()
                        ? "Gricoller charges / needed:"
                        : "Water doses / needed:")
                .right(doses + " / " + needed)
                .rightColor(doses < needed ? new Color(255, 80, 80) : new Color(100, 255, 130))
                .build());
    panelComponent.getChildren().add(TitleComponent.builder().text("REWARD PROGRESS").build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Spendable points:")
                .right(String.valueOf(plugin.getRewardPoints()))
                .rightColor(new Color(255, 205, 60))
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Earned this launch:")
                .right("+" + plugin.getSessionPointsEarned())
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Current 100-fruit set:")
                .right(plugin.getCurrentSetPointsEarned() + " / 35 pts")
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Batches per set:")
                .right(String.valueOf(plugin.getBatchesPerFullSet()))
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Permanent shop:")
                .right(plugin.getPermanentShopProgress() + " / 1150")
                .build());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Full sets to buyout:")
                .right(String.valueOf(plugin.getFullSetsToPermanentBuyout()))
                .rightColor(new Color(255, 205, 60))
                .build());
    reward("Farmer boots", 50);
    reward("Auto-weed", 50);
    reward("Farmer strawhat", 75);
    reward("Farmer trousers", 125);
    reward("Farmer top", 150);
    reward("Gricoller's can", 200);
    reward("Seed box", 250);
    reward("Herb sack", 250);
    panelComponent
        .getChildren()
        .add(LineComponent.builder().left("Repeatables:").right("1 / 2 / 5 / 30 pts").build());
    return super.render(g);
  }

  private void reward(String name, int cost) {
    int remaining = Math.max(0, cost - plugin.getRewardPoints());
    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left(name + " (" + cost + ")")
                .right(remaining == 0 ? "CAN BUY" : remaining + " more")
                .rightColor(remaining == 0 ? new Color(100, 255, 130) : Color.WHITE)
                .build());
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
