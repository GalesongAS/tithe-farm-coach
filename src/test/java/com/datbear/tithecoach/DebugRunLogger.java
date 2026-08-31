package com.datbear.tithecoach;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DebugRunLogger implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(DebugRunLogger.class);
  private static final String HEADER =
      "timestamp,elapsed_seconds,event,method,phase,expected_kind,expected_route_index,"
          + "actual_route_index,transition,out_of_order,harvested_total,inventory_fruit,"
          + "reward_points,session_points_earned,current_set_points,permanent_shop_progress,"
          + "sets_to_buyout,water_doses,water_needed,gricollers_can,active_plants,dry,"
          + "watered,grown,dead,instruction";

  private final TitheFarmCoachPlugin plugin;
  private final Map<WorldPoint, PlantState> previousStates = new HashMap<>();
  private BufferedWriter writer;
  private Instant started;
  private Instant lastSnapshot = Instant.EPOCH;
  private String lastStepKey = "";

  DebugRunLogger(TitheFarmCoachPlugin plugin) {
    this.plugin = plugin;
  }

  void tick() {
    if (!plugin.isInTitheFarm()) {
      close();
      previousStates.clear();
      return;
    }

    ensureStarted();
    recordTransitions();

    CoachStep step = plugin.getStep();
    if (!step.key().equals(lastStepKey)) {
      lastStepKey = step.key();
      write("INSTRUCTION_CHANGE", "", -1, false);
    }

    Instant now = Instant.now();
    if (Duration.between(lastSnapshot, now).getSeconds() >= 5) {
      lastSnapshot = now;
      write("SNAPSHOT", "", -1, false);
    }
  }

  private void ensureStarted() {
    if (writer != null) {
      return;
    }

    try {
      Path directory = RuneLite.RUNELITE_DIR.toPath().resolve("tithe-farm-coach-runs");
      Files.createDirectories(directory);
      started = Instant.now();
      String stamp =
          DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
              .withZone(ZoneId.systemDefault())
              .format(started);
      Path file = directory.resolve("tithe-coach-" + stamp + ".csv");
      writer =
          Files.newBufferedWriter(
              file,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE_NEW,
              StandardOpenOption.WRITE);
      writer.write(HEADER);
      writer.newLine();
      write("SESSION_START", "", -1, false);
    } catch (IOException ex) {
      LOG.warn("Unable to open debug run log", ex);
      close();
    }
  }

  private void recordTransitions() {
    for (Map.Entry<WorldPoint, TithePlot> entry : plugin.getPlots().entrySet()) {
      WorldPoint point = entry.getKey();
      PlantState next = entry.getValue().state;
      PlantState previous = previousStates.put(point, next);
      if (previous == null || previous == next) {
        continue;
      }

      String action = observedAction(previous, next);
      if (action == null) {
        continue;
      }

      int actual = plugin.getRoute().indexOf(point);
      int expected = plugin.expectedRouteIndex();
      boolean orderSensitive =
          action.equals("PLANT") || action.equals("WATER") || action.equals("HARVEST");
      boolean outOfOrder = orderSensitive && actual >= 0 && expected >= 0 && actual != expected;
      write(
          outOfOrder ? "OUT_OF_ORDER" : "OBSERVED_ACTION",
          action + ":" + previous + "->" + next,
          actual,
          outOfOrder);
    }
  }

  private String observedAction(PlantState previous, PlantState next) {
    if (previous == PlantState.EMPTY && (next == PlantState.DRY || next == PlantState.WATERED)) {
      return "PLANT";
    }
    if (previous == PlantState.DRY && next == PlantState.WATERED) {
      return "WATER";
    }
    if (previous == PlantState.GROWN && next == PlantState.EMPTY) {
      return "HARVEST";
    }
    if (previous == PlantState.DEAD && next == PlantState.EMPTY) {
      return "CLEAR_DEAD";
    }
    if (previous != PlantState.DEAD && next == PlantState.DEAD) {
      return "PLANT_DIED";
    }
    return null;
  }

  private void write(String event, String transition, int actual, boolean outOfOrder) {
    if (writer == null) {
      return;
    }

    try {
      CoachStep step = plugin.getStep();
      int dry = 0;
      int watered = 0;
      int grown = 0;
      int dead = 0;
      int active = 0;
      for (WorldPoint point : plugin.getRoute()) {
        TithePlot plot = plugin.getPlots().get(point);
        if (plot == null) {
          continue;
        }
        if (plot.state != PlantState.EMPTY) {
          active++;
        }
        if (plot.state == PlantState.DRY) {
          dry++;
        } else if (plot.state == PlantState.WATERED) {
          watered++;
        } else if (plot.state == PlantState.GROWN) {
          grown++;
        } else if (plot.state == PlantState.DEAD) {
          dead++;
        }
      }

      String[] values = {
        Instant.now().toString(),
        String.valueOf(Duration.between(started, Instant.now()).getSeconds()),
        event,
        plugin.methodName(),
        plugin.phaseName(),
        step.kind.name(),
        String.valueOf(plugin.expectedRouteIndex()),
        String.valueOf(actual),
        transition,
        String.valueOf(outOfOrder),
        String.valueOf(plugin.getHarvestedTotal()),
        String.valueOf(plugin.inventoryFruitCount()),
        String.valueOf(plugin.getRewardPoints()),
        String.valueOf(plugin.getSessionPointsEarned()),
        String.valueOf(plugin.getCurrentSetPointsEarned()),
        String.valueOf(plugin.getPermanentShopProgress()),
        String.valueOf(plugin.getFullSetsToPermanentBuyout()),
        String.valueOf(plugin.getWaterDoseCount()),
        String.valueOf(plugin.getRemainingWaterNeeded()),
        String.valueOf(plugin.isUsingGricollersCan()),
        String.valueOf(active),
        String.valueOf(dry),
        String.valueOf(watered),
        String.valueOf(grown),
        String.valueOf(dead),
        step.title + ". " + step.detail
      };

      for (int index = 0; index < values.length; index++) {
        if (index > 0) {
          writer.write(',');
        }
        writer.write(csv(values[index]));
      }
      writer.newLine();
      writer.flush();
    } catch (IOException ex) {
      LOG.warn("Unable to write debug run log", ex);
      close();
    }
  }

  private String csv(String value) {
    return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
  }

  @Override
  public void close() {
    if (writer == null) {
      return;
    }
    try {
      writer.close();
    } catch (IOException ex) {
      LOG.debug("Unable to close debug run log", ex);
    } finally {
      writer = null;
      lastStepKey = "";
      lastSnapshot = Instant.EPOCH;
    }
  }
}
