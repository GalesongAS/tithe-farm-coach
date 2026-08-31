package com.datbear.tithecoach;

import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

final class WateringPlanner {
  private WateringPlanner() {}

  static WorldPoint nextDry(
      List<WorldPoint> route,
      Map<WorldPoint, TithePlot> plots,
      int lastCompletedRouteIndex) {
    if (route.isEmpty()) {
      return null;
    }
    int start = Math.floorMod(lastCompletedRouteIndex + 1, route.size());
    for (int offset = 0; offset < route.size(); offset++) {
      WorldPoint candidate = route.get((start + offset) % route.size());
      if (isDry(plots, candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private static boolean isDry(Map<WorldPoint, TithePlot> plots, WorldPoint point) {
    TithePlot plot = plots.get(point);
    return plot != null && plot.state == PlantState.DRY;
  }

}
