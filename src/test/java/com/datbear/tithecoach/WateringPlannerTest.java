package com.datbear.tithecoach;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class WateringPlannerTest {
  private static final Instant NOW = Instant.parse("2026-08-31T15:18:12Z");

  @Test
  public void newlyDryFirstPatchDoesNotInterruptForwardCircuit() {
    Fixture fixture = new Fixture(10);
    fixture.dry(0, 1);
    fixture.dry(6, 5);

    assertEquals(fixture.route.get(6), fixture.nextAfter(5));
  }

  @Test
  public void wrapsAroundAfterFinishingEndOfCircuit() {
    Fixture fixture = new Fixture(10);
    fixture.dry(0, 5);
    fixture.dry(3, 5);

    assertEquals(fixture.route.get(0), fixture.nextAfter(8));
  }

  @Test
  public void genuinelyEndangeredPlantOverridesCircuit() {
    Fixture fixture = new Fixture(10);
    fixture.dry(0, 50);
    fixture.dry(6, 5);

    assertEquals(fixture.route.get(0), fixture.nextAfter(5));
  }

  private static final class Fixture {
    private final List<WorldPoint> route;
    private final Map<WorldPoint, TithePlot> plots = new HashMap<>();

    private Fixture(int size) {
      WorldPoint[] points = new WorldPoint[size];
      for (int index = 0; index < size; index++) {
        points[index] = new WorldPoint(1000 + index, 2000, 0);
        plots.put(points[index], new TithePlot(points[index], null, PlantState.WATERED));
      }
      route = Arrays.asList(points);
    }

    private void dry(int index, long secondsAgo) {
      TithePlot plot = plots.get(route.get(index));
      plot.state = PlantState.DRY;
      plot.stageStarted = NOW.minusSeconds(secondsAgo);
    }

    private WorldPoint nextAfter(int index) {
      return WateringPlanner.nextDry(route, plots, index, NOW);
    }
  }
}
