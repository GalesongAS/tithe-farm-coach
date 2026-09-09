package com.galesong.tithecoach;

import java.time.Instant;
import java.util.*;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

final class CoachEngine {
  static final int FRUIT_PER_FULL_SET = 100;
  static final int POINTS_PER_FULL_SET = 35;
  static final int PERMANENT_REWARD_TOTAL = 1150;
  static final int WATER_DOSES_PER_PLANT = 3;

  private enum Phase {
    PLANTING,
    GROWING,
    HARVESTING,
    BETWEEN_BATCHES
  }

  final Map<WorldPoint, TithePlot> plots = new HashMap<>();
  final List<WorldPoint> route = new ArrayList<>();
  CoachStep step =
      new CoachStep(
          CoachStep.Kind.PREPARE,
          "PREPARE",
          "Travel to Tithe Farm with the required supplies.",
          null,
          -1);
  private int harvestedTotal = 0;
  private Phase phase = Phase.PLANTING;
  private int lastCompletedRouteIndex = -1;
  private int rewardPoints = 0;
  private int lastRewardPoints = -1;
  private int sessionPointsEarned = 0;
  private int setPointBaseline = -1;
  WorldPoint depositSack;
  WorldPoint waterSource;

  private TitheMethod method = TitheMethod.STANDARD_20;
  TitheInventory inventory = new TitheInventory(null, 0);

  void changeMethod(TitheMethod method) {
    this.method = method;
    route.clear();
    phase = Phase.PLANTING;
    lastCompletedRouteIndex = -1;
  }

  void beginSet() {
    if (setPointBaseline < 0) setPointBaseline = sessionPointsEarned;
  }

  int requiredWaterDoses() {
    return method.plants * WATER_DOSES_PER_PLANT;
  }

  void updatePlot(WorldPoint point, GameObject object, PlantState state, Instant now) {
    TithePlot plot = plots.get(point);
    PlantState previous = plot == null ? PlantState.EMPTY : plot.state;
    if (plot == null) {
      plot = new TithePlot(point, object, state);
      plots.put(point, plot);
    } else {
      plot.object = object;
      plot.state = state;
    }
    if (previous == PlantState.EMPTY && state != PlantState.EMPTY) {
      plot.stageStarted = now;
      plot.wateringCount = 0;
    }
    if (previous == PlantState.WATERED && state == PlantState.DRY) plot.stageStarted = now;
    if (previous == PlantState.DRY && state == PlantState.WATERED) plot.wateringCount++;
    if (state == PlantState.EMPTY) plot.wateringCount = 0;
    int routeIndex = route.indexOf(point);
    if (routeIndex >= 0
        && ((previous == PlantState.DRY && state == PlantState.WATERED)
            || (previous == PlantState.GROWN && state == PlantState.EMPTY))) {
      lastCompletedRouteIndex = routeIndex;
    }
    if (previous == PlantState.GROWN && state == PlantState.EMPTY && routeIndex >= 0)
      harvestedTotal++;
  }

  CoachStep preparationStep() {
    if (!inventory.hasItem(ItemID.SPADE))
      return prep("GET A SPADE", "You need one spade.", ItemID.SPADE);
    if (!inventory.hasItem(ItemID.DIBBER))
      return prep(
          "GET A SEED DIBBER", "Required unless bare-handed planting is unlocked.", ItemID.DIBBER);
    if (!inventory.hasSeed())
      return prep(
          "TAKE TITHE SEEDS", "Use the highest seed your Farming level allows: 34, 54, or 74.", -1);
    int doses = inventory.waterDoseCount();
    int required = requiredWaterDoses();
    if (doses < required)
      return prep(
          "FILL WATERING CANS",
          "Method needs at least "
              + required
              + " water doses; detected "
              + doses
              + ". "
              + (inventory.hasGricollersCan()
                  ? "Refill Gricoller's can."
                  : "Fill every can completely."),
          inventory.firstWateringCan());
    return prep("ENTER TITHE FARM", "Supplies ready for " + method + ".", inventory.seedId());
  }

  private CoachStep prep(String title, String detail, int item) {
    return new CoachStep(CoachStep.Kind.PREPARE, title, detail, null, item);
  }

  // Returns null only when the next batch needs a freshly mapped route.
  CoachStep decide() {
    boolean active = hasActivePlants();
    // This is a pre-batch check only. After harvesting, fruit must be
    // deposited before water preparation is allowed to replace the advice.
    if (!active
        && inventory.fruitCount() == 0
        && phase == Phase.PLANTING
        && inventory.waterDoseCount() < requiredWaterDoses()) {
      int required = requiredWaterDoses();
      return new CoachStep(
          CoachStep.Kind.PREPARE,
          "NOT ENOUGH WATER IN INVENTORY",
          "Do not plant patch 1. "
              + method
              + " requires "
              + required
              + " doses, but your inventory contains only "
              + inventory.waterDoseCount()
              + ". Leave and bring enough filled cans, or select a smaller method.",
          null,
          -1);
    }
    if (active && inventory.waterDoseCount() == 0)
      return new CoachStep(
          CoachStep.Kind.REFILL,
          "OUT OF WATER - REFILL NOW",
          "No water doses remain. Refill immediately, then the coach will resume the endangered"
              + " plants.",
          waterSource,
          inventory.firstWateringCan());
    if (phase == Phase.PLANTING) {
      // Finish the initial plant-and-water pair before servicing older plants
      // which have already advanced to their next growth stage.
      for (WorldPoint p : route) {
        PlantState s = plot(p).state;
        if (s == PlantState.DRY && plot(p).wateringCount == 0)
          return water(p, "Water this newly planted seed immediately before moving on.");
      }
      for (WorldPoint p : route)
        if (plot(p).state == PlantState.EMPTY)
          return new CoachStep(
              CoachStep.Kind.PLANT,
              "PLANT PATCH " + (route.indexOf(p) + 1),
              "Plant the highlighted patch, then wait for the water instruction.",
              p,
              inventory.seedId());
      phase = Phase.GROWING;
    }

    if (phase == Phase.GROWING) {
      // Keep moving forward through the watering circuit. Cross-farm
      // emergency redirects split the lap and leave groups of plants behind.
      WorldPoint nextDry = nextInRouteWithState(PlantState.DRY);
      if (nextDry != null)
        return water(
            nextDry, "Continue forward through the watering circuit without backtracking.");
      WorldPoint ripe = nextInRouteWithState(PlantState.GROWN);
      if (ripe != null)
        return new CoachStep(
            CoachStep.Kind.HARVEST,
            "HARVEST PATCH " + (route.indexOf(ripe) + 1),
            "This plant is ready now. Harvest it instead of waiting.",
            ripe,
            ItemID.SPADE);
      WorldPoint unfinished = nextUnfinishedPlant();
      if (unfinished != null)
        return new CoachStep(
            CoachStep.Kind.WAIT,
            "MOVE TO NEXT PLANT",
            "Move forward to the highlighted next plant and wait for it to need water.",
            unfinished,
            -1);
      phase = Phase.HARVESTING;
    }

    if (phase == Phase.HARVESTING) {
      WorldPoint ripe = nextInRouteWithState(PlantState.GROWN);
      if (ripe != null)
        return new CoachStep(
            CoachStep.Kind.HARVEST,
            "HARVEST PATCH " + (route.indexOf(ripe) + 1),
            "Harvest forward in route order.",
            ripe,
            ItemID.SPADE);
      WorldPoint dead = nextInRouteWithState(PlantState.DEAD);
      if (dead != null)
        return new CoachStep(
            CoachStep.Kind.RECOVER,
            "CLEAR DEAD PLANT",
            "Living plants are safe. Clear this dead plant now.",
            dead,
            ItemID.SPADE);
      phase = Phase.BETWEEN_BATCHES;
    }

    int fruit = inventory.fruitCount();
    if (fruit > 0)
      return new CoachStep(
          CoachStep.Kind.DEPOSIT,
          "DEPOSIT FRUIT",
          "Deposit the " + fruit + " fruit in your inventory.",
          depositSack,
          inventory.fruitId());
    if (inventory.waterDoseCount() < requiredWaterDoses())
      return new CoachStep(
          CoachStep.Kind.REFILL,
          inventory.hasGricollersCan() ? "REFILL GRICOLLER'S CAN" : "REFILL WATERING CANS",
          (inventory.hasGricollersCan()
                  ? "Refill Gricoller's can at the water source. "
                  : "Fill every can. ")
              + "The next batch needs "
              + requiredWaterDoses()
              + " doses.",
          waterSource,
          inventory.firstWateringCan());
    if (harvestedTotal >= FRUIT_PER_FULL_SET) {
      return new CoachStep(
          CoachStep.Kind.COMPLETE,
          "100 FRUIT COMPLETE",
          "Excellent. Your full Tithe Farm set is complete.",
          null,
          -1);
    }
    phase = Phase.PLANTING;
    route.clear();
    lastCompletedRouteIndex = -1;
    // The integration rebuilds the next batch route and decides again in the same tick.
    return null;
  }

  private CoachStep water(WorldPoint p, String detail) {
    return new CoachStep(
        CoachStep.Kind.WATER,
        "WATER PATCH " + (route.indexOf(p) + 1),
        detail,
        p,
        inventory.firstWateringCan());
  }

  private TithePlot plot(WorldPoint p) {
    return plots.get(p);
  }

  private WorldPoint nextInRouteWithState(PlantState wanted) {
    if (route.isEmpty()) return null;
    int start = (lastCompletedRouteIndex + 1 + route.size()) % route.size();
    for (int offset = 0; offset < route.size(); offset++) {
      WorldPoint candidate = route.get((start + offset) % route.size());
      if (plot(candidate).state == wanted) return candidate;
    }
    return null;
  }

  private WorldPoint nextUnfinishedPlant() {
    if (route.isEmpty()) return null;
    int start = (lastCompletedRouteIndex + 1 + route.size()) % route.size();
    for (int offset = 0; offset < route.size(); offset++) {
      WorldPoint candidate = route.get((start + offset) % route.size());
      PlantState state = plot(candidate).state;
      if (state == PlantState.DRY || state == PlantState.WATERED) return candidate;
    }
    return null;
  }

  WorldPoint getQueuedPlantTarget() {
    if (phase != Phase.PLANTING || step.kind != CoachStep.Kind.WATER || step.target == null)
      return null;
    int current = route.indexOf(step.target);
    if (current < 0) return null;
    for (int offset = 1; offset < route.size(); offset++) {
      WorldPoint candidate = route.get((current + offset) % route.size());
      if (plot(candidate).state == PlantState.EMPTY) return candidate;
    }
    return null;
  }

  boolean isQueuedPlantClickReady() {
    if (step.kind != CoachStep.Kind.WATER || step.target == null) return false;
    TithePlot current = plot(step.target);
    return current != null && current.state == PlantState.WATERED;
  }

  int getRemainingWaterNeeded() {
    if (route.isEmpty()) return requiredWaterDoses();
    int needed = 0;
    for (WorldPoint p : route) {
      TithePlot plot = plot(p);
      if (plot.state == PlantState.EMPTY && phase == Phase.PLANTING)
        needed += WATER_DOSES_PER_PLANT;
      else if (plot.state == PlantState.DRY || plot.state == PlantState.WATERED)
        needed += Math.max(0, WATER_DOSES_PER_PLANT - plot.wateringCount);
    }
    return needed;
  }

  boolean isBatchBlockedByWater(int waterDoses) {
    return !route.isEmpty() && !hasActivePlants() && waterDoses < requiredWaterDoses();
  }

  private boolean hasActivePlants() {
    for (WorldPoint point : route) {
      PlantState state = plot(point).state;
      if (state != PlantState.EMPTY && state != PlantState.DEAD) return true;
    }
    return false;
  }

  int getHarvestedTotal() {
    return harvestedTotal;
  }

  int getRewardPoints() {
    return rewardPoints;
  }

  int getSessionPointsEarned() {
    return sessionPointsEarned;
  }

  int getCurrentSetPointsEarned() {
    return Math.max(0, sessionPointsEarned - Math.max(0, setPointBaseline));
  }

  int getPermanentShopProgress(int permanentPointsSpent) {
    return Math.min(PERMANENT_REWARD_TOTAL, rewardPoints + permanentPointsSpent);
  }

  int getFullSetsToPermanentBuyout(int permanentPointsSpent) {
    return (Math.max(0, PERMANENT_REWARD_TOTAL - getPermanentShopProgress(permanentPointsSpent))
            + POINTS_PER_FULL_SET
            - 1)
        / POINTS_PER_FULL_SET;
  }

  int getBatchesPerFullSet() {
    return (FRUIT_PER_FULL_SET + method.plants - 1) / method.plants;
  }

  String phaseName() {
    return phase.name();
  }

  void updateRewardPoints(int current) {
    if (lastRewardPoints >= 0 && current > lastRewardPoints)
      sessionPointsEarned += current - lastRewardPoints;
    rewardPoints = current;
    lastRewardPoints = current;
  }

  void reset() {
    harvestedTotal = 0;
    plots.clear();
    route.clear();
    depositSack = null;
    waterSource = null;
    phase = Phase.PLANTING;
    lastCompletedRouteIndex = -1;
    setPointBaseline = -1;
  }
}
