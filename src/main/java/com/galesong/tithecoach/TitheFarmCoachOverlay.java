package com.galesong.tithecoach;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

final class TitheFarmCoachOverlay extends Overlay {
  private final Client client;
  private final TitheFarmCoachPlugin plugin;
  private final TitheFarmCoachConfig config;

  @Inject
  TitheFarmCoachOverlay(Client client, TitheFarmCoachPlugin plugin, TitheFarmCoachConfig config) {
    this.client = client;
    this.plugin = plugin;
    this.config = config;
    setPosition(OverlayPosition.DYNAMIC);
    setLayer(OverlayLayer.ABOVE_WIDGETS);
    setPriority(Overlay.PRIORITY_HIGH);
  }

  @Override
  public Dimension render(Graphics2D g) {
    if (!plugin.isInTitheFarm()) {
      return null;
    }

    CoachStep step = plugin.getStep();
    List<WorldPoint> route = plugin.getRoute();
    if (config.showRouteNumbers()) {
      for (int i = 0; i < route.size(); i++) {
        WorldPoint point = route.get(i);
        drawPlot(g, point, routeStateColor(point), String.valueOf(i + 1), false);
      }
    }
    if (plugin.isBatchBlockedByWater() && !route.isEmpty())
      drawPlot(g, route.get(0), new Color(255, 45, 45), "1 - NOT ENOUGH WATER", true);
    if (config.readableTimers()) for (WorldPoint point : route) drawReadableTimer(g, point);
    if (step.target != null)
      drawPlot(g, step.target, color(step.kind), actionLabel(step.kind), true);
    WorldPoint queued = plugin.getQueuedPlantTarget();
    if (queued != null) {
      boolean ready = plugin.isQueuedPlantClickReady();
      drawPlot(
          g,
          queued,
          ready ? new Color(70, 255, 110) : new Color(255, 65, 65),
          ready ? "READY" : "WAIT",
          true);
      if (plugin.getSeedItemId() >= 0)
        highlightInventory(g, plugin.getSeedItemId(), new Color(255, 205, 60));
    }
    // Watering is performed directly on the plant; highlighting a can implies
    // an unnecessary use-item action. Keep inventory guidance for other steps.
    if (step.inventoryItem >= 0 && step.kind != CoachStep.Kind.WATER)
      highlightInventory(g, step.inventoryItem, color(step.kind));
    return null;
  }

  private void drawReadableTimer(Graphics2D g, WorldPoint world) {
    TithePlot plot = plugin.getPlots().get(world);
    if (plot == null
        || (plot.state != PlantState.DRY && plot.state != PlantState.WATERED)
        || plot.stageStarted.equals(Instant.EPOCH)) return;
    LocalPoint local = LocalPoint.fromWorld(client, world);
    if (local == null) return;
    int plane = client.getTopLevelWorldView().getPlane();
    net.runelite.api.Point canvas = Perspective.localToCanvas(client, local, plane, 95);
    if (canvas == null) return;
    long elapsed = Math.max(0, Duration.between(plot.stageStarted, Instant.now()).toMillis());
    int remaining = (int) Math.max(0, 60 - (elapsed / 1000));
    double progress = Math.min(1.0, elapsed / 60000.0);
    boolean dry = plot.state == PlantState.DRY;
    Color urgency =
        dry
            ? (remaining <= 15 ? new Color(255, 45, 45) : new Color(255, 170, 25))
            : new Color(50, 235, 125);
    String text = (dry ? "DIE " : "WATER ") + remaining + "s";
    int width = 74, height = 13, x = canvas.getX() - width / 2, y = canvas.getY() - height / 2;
    g.setColor(new Color(0, 0, 0, 220));
    g.fillRoundRect(x - 3, y - 3, width + 6, height + 6, 6, 6);
    g.setColor(new Color(35, 35, 35, 240));
    g.fillRect(x, y, width, height);
    g.setColor(urgency);
    g.fillRect(x, y, (int) Math.round(width * progress), height);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2));
    g.drawRect(x, y, width, height);
    FontMetrics fm = g.getFontMetrics();
    g.setColor(Color.BLACK);
    g.drawString(text, x + (width - fm.stringWidth(text)) / 2 + 1, y + height - 1);
    g.setColor(Color.WHITE);
    g.drawString(text, x + (width - fm.stringWidth(text)) / 2, y + height - 2);
  }

  private Color routeStateColor(WorldPoint world) {
    TithePlot plot = plugin.getPlots().get(world);
    if (plot == null || plot.state == PlantState.EMPTY) return new Color(70, 150, 255);
    if (plot.state == PlantState.WATERED) return new Color(45, 225, 115);
    if (plot.state == PlantState.GROWN) return new Color(205, 100, 255);
    if (plot.state == PlantState.DEAD) return new Color(125, 25, 25);
    if (plot.state == PlantState.DRY) {
      if (plot.stageStarted.equals(Instant.EPOCH)) return new Color(255, 170, 25);
      long elapsed = Duration.between(plot.stageStarted, Instant.now()).getSeconds();
      return elapsed >= 45 ? new Color(255, 45, 45) : new Color(255, 170, 25);
    }
    return new Color(70, 150, 255);
  }

  private String actionLabel(CoachStep.Kind kind) {
    switch (kind) {
      case PLANT:
        return "PLANT";
      case WATER:
        return "WATER";
      case HARVEST:
        return "HARVEST";
      case DEPOSIT:
        return "DEPOSIT";
      case REFILL:
        return "REFILL";
      case RECOVER:
        return "CLEAR";
      case WAIT:
        return "WAIT";
      case COMPLETE:
        return "DONE";
      default:
        return "PREPARE";
    }
  }

  private void drawPlot(Graphics2D g, WorldPoint world, Color color, String label, boolean strong) {
    TithePlot plot = plugin.getPlots().get(world);
    GameObject object = plot == null ? targetObject(world) : plot.object;
    if (object == null) return;
    Shape hull = object.getConvexHull();
    if (hull != null)
      OverlayUtil.renderPolygon(
          g,
          hull,
          color,
          new Color(color.getRed(), color.getGreen(), color.getBlue(), strong ? 80 : 45),
          new BasicStroke(strong ? 4 : 2));
    LocalPoint local = LocalPoint.fromWorld(client, world);
    if (local == null) return;
    int plane = client.getTopLevelWorldView().getPlane();
    net.runelite.api.Point point = Perspective.localToCanvas(client, local, plane, 30);
    if (point != null)
      OverlayUtil.renderTextLocation(g, point, label, strong ? Color.WHITE : color);
  }

  private GameObject targetObject(WorldPoint p) {
    if (plugin.getDepositSack() != null && p.equals(plugin.getDepositSack().getWorldLocation()))
      return plugin.getDepositSack();
    if (plugin.getWaterSource() != null && p.equals(plugin.getWaterSource().getWorldLocation()))
      return plugin.getWaterSource();
    return null;
  }

  private void highlightInventory(Graphics2D g, int id, Color color) {
    Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
    if (inventory == null || inventory.isHidden()) return;
    Widget[] children = inventory.getDynamicChildren();
    if (children == null) return;
    for (Widget item : children)
      if (item.getItemId() == id) {
        Rectangle r = item.getBounds();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
        g.fill(r);
        g.setColor(color);
        g.setStroke(new BasicStroke(3));
        g.draw(r);
      }
  }

  private Color color(CoachStep.Kind kind) {
    if (kind == CoachStep.Kind.RECOVER) return new Color(255, 70, 70);
    if (kind == CoachStep.Kind.WAIT) return new Color(255, 205, 60);
    if (kind == CoachStep.Kind.COMPLETE) return new Color(100, 255, 120);
    return new Color(70, 255, 140);
  }
}
