package com.galesong.tithecoach;

import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class TitheRoutes {
  static final int TITHE_FARM_REGION = 7222;
  static final int PATCH_COUNT = 25;

  private TitheRoutes() {}

  private static final int[][] SIMPLE_ROUTE = {
    {24, 57}, {29, 57}, {24, 54}, {29, 54}, {24, 51}, {29, 51}, {29, 48}, {29, 42}, {29, 39},
    {29, 36}, {29, 33}, {29, 27}, {19, 33}, {19, 36}, {24, 39}, {19, 39}, {24, 42}, {19, 42},
    {24, 48}, {19, 48}, {19, 51}, {19, 54}, {19, 57}
  };

  private static final int[][] ADVANCED_ROUTE = {
    {27, 43}, {26, 41}, {26, 39}, {26, 37}, {27, 35}, {26, 33}, {28, 29}, {32, 32}, {31, 34},
    {32, 36}, {32, 38}, {31, 40}, {32, 42}, {32, 48}, {32, 50}, {31, 52}, {32, 54}, {32, 56},
    {31, 58}, {26, 58}, {26, 55}, {27, 53}, {26, 51}, {26, 49}, {27, 47}
  };

  private static final int[][] STANDARD_ROUTE = {
    {21, 56}, {22, 56}, {22, 55}, {21, 54}, {21, 52}, {22, 51}, {22, 49}, {21, 47}, {21, 43},
    {22, 42}, {22, 40}, {21, 39}, {21, 37}, {22, 36}, {22, 34}, {21, 32}, {27, 32}, {27, 36},
    {27, 38}, {27, 42}
  };

  static int[][] officialRoute(TitheMethod method) {
    switch (method) {
      case RELAXED_8:
      case SAFE_16:
      case STANDARD_20:
        // Practice methods use the first N patches of the standard circuit.
        return STANDARD_ROUTE;
      case SIMPLE_23:
        return SIMPLE_ROUTE;
      case ADVANCED_25:
        return ADVANCED_ROUTE;
      default:
        throw new IllegalArgumentException("Unknown Tithe method: " + method);
    }
  }

  static List<WorldPoint> build(
      Client client, TitheMethod method, Map<WorldPoint, TithePlot> plots, int harvestedTotal) {
    Player player = client.getLocalPlayer();
    if (player == null) return Collections.emptyList();
    List<WorldPoint> guides = new ArrayList<>();
    for (int[] marker : officialRoute(method)) {
      WorldPoint template = WorldPoint.fromRegion(TITHE_FARM_REGION, marker[0], marker[1], 0);
      Collection<WorldPoint> instances =
          WorldPoint.toLocalInstance(client.getTopLevelWorldView(), template);
      guides.add(
          instances.stream()
              .min(Comparator.comparingInt(p -> distance(player.getWorldLocation(), p)))
              .orElse(template));
    }
    return build(method, plots, harvestedTotal, guides);
  }

  static List<WorldPoint> build(
      TitheMethod method,
      Map<WorldPoint, TithePlot> plots,
      int harvestedTotal,
      List<WorldPoint> guides) {
    List<WorldPoint> route = new ArrayList<>();
    int needed =
        Math.min(method.plants, Math.max(1, CoachEngine.FRUIT_PER_FULL_SET - harvestedTotal));
    List<WorldPoint> available = new ArrayList<>();
    // Retain live plants when changing methods; dead plants are excluded as before.
    for (TithePlot plot : plots.values()) {
      if (plot.state != PlantState.DEAD) available.add(plot.location);
    }
    // Every method supplies at least as many markers as its batch size.
    for (WorldPoint guide : guides) {
      if (route.size() >= needed || available.isEmpty()) break;
      WorldPoint next =
          Collections.min(available, Comparator.comparingInt(p -> distance(guide, p)));
      route.add(next);
      available.remove(next);
    }
    return route;
  }

  private static int distance(WorldPoint a, WorldPoint b) {
    return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
  }
}
