package com.galesong.tithecoach;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class TitheRoutesTest {
  private final List<WorldPoint> guides = new ArrayList<>();
  private final Map<WorldPoint, TithePlot> plots = new LinkedHashMap<>();

  public TitheRoutesTest() {
    for (int i = 0; i < 25; i++) {
      WorldPoint point = new WorldPoint(1000 + 5 * i, 2000, 0);
      guides.add(point);
      plots.put(point, new TithePlot(point, null, PlantState.EMPTY));
    }
  }

  @Test
  public void everyMethodHasEnoughMarkersAndPracticeMethodsShareTheStandardRoute() {
    for (TitheMethod method : TitheMethod.values()) {
      assertTrue(TitheRoutes.officialRoute(method).length >= method.plants);
    }
    assertArrayEquals(
        TitheRoutes.officialRoute(TitheMethod.STANDARD_20),
        TitheRoutes.officialRoute(TitheMethod.RELAXED_8));
    assertArrayEquals(
        TitheRoutes.officialRoute(TitheMethod.STANDARD_20),
        TitheRoutes.officialRoute(TitheMethod.SAFE_16));
  }

  @Test
  public void usesMethodSizeAndShortensTheFinalBatch() {
    for (TitheMethod method : TitheMethod.values()) {
      assertEquals(guides.subList(0, method.plants), build(method, 0));
      assertEquals(guides.subList(0, 4), build(method, 96));
    }
  }

  @Test
  public void retainsLivePlantsButExcludesDeadPlantsWhenRebuilding() {
    plots.get(guides.get(0)).state = PlantState.WATERED;
    plots.get(guides.get(1)).state = PlantState.DEAD;
    List<WorldPoint> route = build(TitheMethod.RELAXED_8, 0);
    assertEquals(guides.get(0), route.get(0));
    assertFalse(route.contains(guides.get(1)));
    assertEquals(8, route.size());
    assertEquals(8, new java.util.HashSet<>(route).size());
  }

  private List<WorldPoint> build(TitheMethod method, int harvested) {
    return TitheRoutes.build(method, plots, harvested, guides);
  }
}
