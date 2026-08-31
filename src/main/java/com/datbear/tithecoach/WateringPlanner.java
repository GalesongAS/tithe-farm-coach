package com.datbear.tithecoach;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

final class WateringPlanner {
  static final Duration EMERGENCY_AFTER = Duration.ofSeconds(45);

  private WateringPlanner() {}

  static WorldPoint nextDry(
      List<WorldPoint> route,
      Map<WorldPoint, TithePlot> plots,
      int lastCompletedRouteIndex,
      Instant now) {
    WorldPoint emergency =
        route.stream()
            .filter(point -> isDry(plots, point))
            .filter(point -> isEmergency(plots.get(point), now))
            .min(Comparator.comparing(point -> plots.get(point).stageStarted))
            .orElse(null);
    if (emergency != null) {
      return emergency;
    }

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

  private static boolean isEmergency(TithePlot plot, Instant now) {
    return !plot.stageStarted.equals(Instant.EPOCH)
        && Duration.between(plot.stageStarted, now).compareTo(EMERGENCY_AFTER) >= 0;
  }
}
