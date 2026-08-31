package com.galesong.tithecoach;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Tithe Farm Coach",
    description = "Read-only, one-step-at-a-time Tithe Farm route coach",
    tags = {"tithe", "farm", "farming", "coach"})
public class TitheFarmCoachPlugin extends Plugin {
  private static final int TITHE_FARM_REGION = 7222;

  private enum Phase {
    PLANTING,
    GROWING,
    HARVESTING,
    BETWEEN_BATCHES
  }

  @Inject private Client client;
  @Inject private TitheFarmCoachConfig config;
  @Inject private OverlayManager overlayManager;
  @Inject private TitheFarmCoachOverlay overlay;
  @Inject private TitheFarmCoachPanel panel;

  private final Map<WorldPoint, TithePlot> plots = new HashMap<>();
  private final List<WorldPoint> route = new ArrayList<>();
  private CoachStep step =
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
  private GameObject depositSack;
  private GameObject waterSource;

  Map<WorldPoint, TithePlot> getPlots() {
    return plots;
  }

  List<WorldPoint> getRoute() {
    return route;
  }

  CoachStep getStep() {
    return step;
  }

  int getHarvestedTotal() {
    return harvestedTotal;
  }

  GameObject getDepositSack() {
    return depositSack;
  }

  GameObject getWaterSource() {
    return waterSource;
  }

  private static final Map<Integer, PlantState> PLANT_IDS = new HashMap<>();

  static {
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_EMPTY, PlantState.EMPTY);
    register(
        ObjectID.HOSIDIUS_TITHE_A_4,
        new int[] {
          ObjectID.HOSIDIUS_TITHE_A_1_DRY,
          ObjectID.HOSIDIUS_TITHE_A_1_WET,
          ObjectID.HOSIDIUS_TITHE_A_1_DEAD,
          ObjectID.HOSIDIUS_TITHE_A_2_DRY,
          ObjectID.HOSIDIUS_TITHE_A_2_WET,
          ObjectID.HOSIDIUS_TITHE_A_2_DEAD,
          ObjectID.HOSIDIUS_TITHE_A_3_DRY,
          ObjectID.HOSIDIUS_TITHE_A_3_WET,
          ObjectID.HOSIDIUS_TITHE_A_3_DEAD,
          ObjectID.HOSIDIUS_TITHE_A_4,
          ObjectID.HOSIDIUS_TITHE_A_4_DEAD
        });
    register(
        ObjectID.HOSIDIUS_TITHE_B_4,
        new int[] {
          ObjectID.HOSIDIUS_TITHE_B_1_DRY,
          ObjectID.HOSIDIUS_TITHE_B_1_WET,
          ObjectID.HOSIDIUS_TITHE_B_1_DEAD,
          ObjectID.HOSIDIUS_TITHE_B_2_DRY,
          ObjectID.HOSIDIUS_TITHE_B_2_WET,
          ObjectID.HOSIDIUS_TITHE_B_2_DEAD,
          ObjectID.HOSIDIUS_TITHE_B_3_DRY,
          ObjectID.HOSIDIUS_TITHE_B_3_WET,
          ObjectID.HOSIDIUS_TITHE_B_3_DEAD,
          ObjectID.HOSIDIUS_TITHE_B_4,
          ObjectID.HOSIDIUS_TITHE_B_4_DEAD
        });
    register(
        ObjectID.HOSIDIUS_TITHE_C_4,
        new int[] {
          ObjectID.HOSIDIUS_TITHE_C_1_DRY,
          ObjectID.HOSIDIUS_TITHE_C_1_WET,
          ObjectID.HOSIDIUS_TITHE_C_1_DEAD,
          ObjectID.HOSIDIUS_TITHE_C_2_DRY,
          ObjectID.HOSIDIUS_TITHE_C_2_WET,
          ObjectID.HOSIDIUS_TITHE_C_2_DEAD,
          ObjectID.HOSIDIUS_TITHE_C_3_DRY,
          ObjectID.HOSIDIUS_TITHE_C_3_WET,
          ObjectID.HOSIDIUS_TITHE_C_3_DEAD,
          ObjectID.HOSIDIUS_TITHE_C_4,
          ObjectID.HOSIDIUS_TITHE_C_4_DEAD
        });
  }

  private static void register(int base, int[] ids) {
    for (int id : ids) {
      if (id == base) PLANT_IDS.put(id, PlantState.GROWN);
      else {
        int delta = (base - id) % 3;
        PLANT_IDS.put(
            id, delta == 0 ? PlantState.DRY : delta == 2 ? PlantState.WATERED : PlantState.DEAD);
      }
    }
  }

  @Provides
  TitheFarmCoachConfig provideConfig(ConfigManager manager) {
    return manager.getConfig(TitheFarmCoachConfig.class);
  }

  @Override
  protected void startUp() {
    overlayManager.add(overlay);
    overlayManager.add(panel);
  }

  @Override
  protected void shutDown() {
    overlayManager.remove(overlay);
    overlayManager.remove(panel);
    reset();
  }

  @Subscribe
  public void onGameStateChanged(GameStateChanged e) {
    if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN)
      reset();
  }

  @Subscribe
  public void onConfigChanged(ConfigChanged e) {
    if (e.getGroup().equals("tithefarmcoach") && e.getKey().equals("method")) {
      route.clear();
      phase = Phase.PLANTING;
      lastCompletedRouteIndex = -1;
    }
  }

  @Subscribe
  public void onGameObjectSpawned(GameObjectSpawned e) {
    GameObject object = e.getGameObject();
    PlantState state = PLANT_IDS.get(object.getId());
    if (state != null) updatePlot(object, state);
    ObjectComposition composition = client.getObjectDefinition(object.getId());
    if (composition != null && composition.getActions() != null) {
      for (String action : composition.getActions()) {
        if (action == null) continue;
        if (action.equalsIgnoreCase("Deposit")) depositSack = object;
        if (action.equalsIgnoreCase("Fill")) waterSource = object;
      }
    }
  }

  private void updatePlot(GameObject object, PlantState state) {
    WorldPoint point = object.getWorldLocation();
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
      plot.stageStarted = Instant.now();
      plot.wateringCount = 0;
    }
    if (previous == PlantState.WATERED && state == PlantState.DRY)
      plot.stageStarted = Instant.now();
    if (previous == PlantState.DRY && state == PlantState.WATERED) plot.wateringCount++;
    if (state == PlantState.EMPTY) plot.wateringCount = 0;
    int routeIndex = route.indexOf(point);
    if (routeIndex >= 0
        && ((previous == PlantState.DRY && state == PlantState.WATERED)
            || (previous == PlantState.GROWN && state == PlantState.EMPTY))) {
      lastCompletedRouteIndex = routeIndex;
    }
    if (previous == PlantState.GROWN && state == PlantState.EMPTY && route.contains(point))
      harvestedTotal++;
  }

  @Subscribe
  public void onGameTick(GameTick ignored) {
    updateRewardPoints();
    if (!isInTitheFarm()) {
      if (!plots.isEmpty() || !route.isEmpty()) {
        reset();
      }
      return;
    }
    if (plots.size() < 25) {
      setStep(preparationStep());
      return;
    }
    if (setPointBaseline < 0) setPointBaseline = sessionPointsEarned;
    if (route.isEmpty()) buildRoute();
    if (route.isEmpty()) return;
    setStep(decide());
  }

  private CoachStep preparationStep() {
    if (!hasItem(ItemID.SPADE)) return prep("GET A SPADE", "You need one spade.", ItemID.SPADE);
    if (!hasItem(ItemID.DIBBER))
      return prep(
          "GET A SEED DIBBER", "Required unless bare-handed planting is unlocked.", ItemID.DIBBER);
    if (!hasSeed())
      return prep(
          "TAKE TITHE SEEDS", "Use the highest seed your Farming level allows: 34, 54, or 74.", -1);
    int doses = waterDoseCount();
    int required = config.method().plants * 3;
    if (doses < required)
      return prep(
          "FILL WATERING CANS",
          "Method needs at least "
              + required
              + " water doses; detected "
              + doses
              + ". "
              + (hasGricollersCan() ? "Refill Gricoller's can." : "Fill every can completely."),
          firstWateringCan());
    return prep("ENTER TITHE FARM", "Supplies ready for " + config.method() + ".", seedId());
  }

  private CoachStep prep(String title, String detail, int item) {
    return new CoachStep(CoachStep.Kind.PREPARE, title, detail, null, item);
  }

  private CoachStep decide() {
    int active = 0;
    for (WorldPoint p : route)
      if (plot(p).state != PlantState.EMPTY && plot(p).state != PlantState.DEAD) active++;
    // This is a pre-batch check only. After harvesting, fruit must be
    // deposited before water preparation is allowed to replace the advice.
    if (active == 0
        && fruitCount() == 0
        && phase == Phase.PLANTING
        && waterDoseCount() < config.method().plants * 3) {
      int required = config.method().plants * 3;
      return new CoachStep(
          CoachStep.Kind.PREPARE,
          "NOT ENOUGH WATER IN INVENTORY",
          "Do not plant patch 1. "
              + config.method()
              + " requires "
              + required
              + " doses, but your inventory contains only "
              + waterDoseCount()
              + ". Leave and bring enough filled cans, or select a smaller method.",
          null,
          -1);
    }
    if (active > 0 && waterDoseCount() == 0)
      return new CoachStep(
          CoachStep.Kind.REFILL,
          "OUT OF WATER - REFILL NOW",
          "No water doses remain. Refill immediately, then the coach will resume the endangered"
              + " plants.",
          waterSource == null ? null : waterSource.getWorldLocation(),
          firstWateringCan());
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
              seedId());
      phase = Phase.GROWING;
    }

    if (phase == Phase.GROWING) {
      // Keep moving forward through the watering circuit. Cross-farm
      // emergency redirects split the lap and leave groups of plants behind.
      WorldPoint nextDry = WateringPlanner.nextDry(route, plots, lastCompletedRouteIndex);
      if (nextDry != null)
        return water(
            nextDry,
            "Continue forward through the watering circuit without backtracking.");
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

    int fruit = fruitCount();
    if (fruit > 0)
      return new CoachStep(
          CoachStep.Kind.DEPOSIT,
          "DEPOSIT FRUIT",
          "Deposit the " + fruit + " fruit in your inventory.",
          depositSack == null ? null : depositSack.getWorldLocation(),
          fruitId());
    if (waterDoseCount() < config.method().plants * 3)
      return new CoachStep(
          CoachStep.Kind.REFILL,
          hasGricollersCan() ? "REFILL GRICOLLER'S CAN" : "REFILL WATERING CANS",
          (hasGricollersCan() ? "Refill Gricoller's can at the water source. " : "Fill every can. ")
              + "The next batch needs "
              + (config.method().plants * 3)
              + " doses.",
          waterSource == null ? null : waterSource.getWorldLocation(),
          firstWateringCan());
    if (harvestedTotal >= 100) {
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
    buildRoute();
    return decide();
  }

  private CoachStep water(WorldPoint p, String detail) {
    return new CoachStep(
        CoachStep.Kind.WATER,
        "WATER PATCH " + (route.indexOf(p) + 1),
        detail,
        p,
        firstWateringCan());
  }

  private TithePlot plot(WorldPoint p) {
    return plots.get(p);
  }

  private void buildRoute() {
    Player player = client.getLocalPlayer();
    if (player == null) return;
    int needed = Math.min(config.method().plants, Math.max(1, 100 - harvestedTotal));
    List<WorldPoint> available = new ArrayList<>();
    // Include live plants when rebuilding, so changing methods can expand
    // the route without forgetting the plants already in progress.
    for (TithePlot p : plots.values()) if (p.state != PlantState.DEAD) available.add(p.location);
    int[][] official = officialRoute(config.method());
    if (official != null) {
      for (int[] marker : official) {
        if (route.size() >= needed || available.isEmpty()) break;
        WorldPoint templateGuide = WorldPoint.fromRegion(7222, marker[0], marker[1], 0);
        Collection<WorldPoint> instanceGuides =
            WorldPoint.toLocalInstance(client.getTopLevelWorldView(), templateGuide);
        WorldPoint guide =
            instanceGuides.stream()
                .min(Comparator.comparingInt(p -> distance(player.getWorldLocation(), p)))
                .orElse(templateGuide);
        WorldPoint next =
            Collections.min(available, Comparator.comparingInt(p -> distance(guide, p)));
        route.add(next);
        available.remove(next);
      }
      if (route.size() >= needed) return;
    }
    WorldPoint cursor = player.getWorldLocation();
    while (!available.isEmpty() && route.size() < needed) {
      final WorldPoint from = cursor;
      WorldPoint next = Collections.min(available, Comparator.comparingInt(p -> distance(from, p)));
      route.add(next);
      available.remove(next);
      cursor = next;
    }
  }

  private int[][] officialRoute(TitheMethod method) {
    if (method == TitheMethod.SIMPLE_23)
      return new int[][] {
        {24, 57}, {29, 57}, {24, 54}, {29, 54}, {24, 51}, {29, 51}, {29, 48}, {29, 42}, {29, 39},
        {29, 36}, {29, 33}, {29, 27}, {19, 33}, {19, 36}, {24, 39}, {19, 39}, {24, 42}, {19, 42},
        {24, 48}, {19, 48}, {19, 51}, {19, 54}, {19, 57}
      };
    if (method == TitheMethod.ADVANCED_25)
      return new int[][] {
        {27, 43}, {26, 41}, {26, 39}, {26, 37}, {27, 35}, {26, 33}, {28, 29}, {32, 32}, {31, 34},
        {32, 36}, {32, 38}, {31, 40}, {32, 42}, {32, 48}, {32, 50}, {31, 52}, {32, 54}, {32, 56},
        {31, 58}, {26, 58}, {26, 55}, {27, 53}, {26, 51}, {26, 49}, {27, 47}
      };
    return new int[][] {
      {21, 56}, {22, 56}, {22, 55}, {21, 54}, {21, 52}, {22, 51}, {22, 49}, {21, 47}, {21, 43},
      {22, 42}, {22, 40}, {21, 39}, {21, 37}, {22, 36}, {22, 34}, {21, 32}, {27, 32}, {27, 36},
      {27, 38}, {27, 42}
    };
  }

  private int distance(WorldPoint a, WorldPoint b) {
    return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
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

  private void setStep(CoachStep next) {
    step = next;
  }

  boolean hasItem(int id) {
    ItemContainer inv = client.getItemContainer(InventoryID.INV);
    return inv != null && inv.count(id) > 0;
  }

  private boolean hasSeed() {
    return seedId() != -1;
  }

  private int seedId() {
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_C)) return ItemID.HOSIDIUS_TITHE_SEED_C;
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_B)) return ItemID.HOSIDIUS_TITHE_SEED_B;
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_A)) return ItemID.HOSIDIUS_TITHE_SEED_A;
    return -1;
  }

  private int fruitId() {
    if (hasItem(ItemID.HOSIDIUS_TITHE_FRUIT_C)) return ItemID.HOSIDIUS_TITHE_FRUIT_C;
    if (hasItem(ItemID.HOSIDIUS_TITHE_FRUIT_B)) return ItemID.HOSIDIUS_TITHE_FRUIT_B;
    return ItemID.HOSIDIUS_TITHE_FRUIT_A;
  }

  private int fruitCount() {
    ItemContainer inv = client.getItemContainer(InventoryID.INV);
    if (inv == null) return 0;
    return inv.count(ItemID.HOSIDIUS_TITHE_FRUIT_A)
        + inv.count(ItemID.HOSIDIUS_TITHE_FRUIT_B)
        + inv.count(ItemID.HOSIDIUS_TITHE_FRUIT_C);
  }

  int inventoryFruitCount() {
    return fruitCount();
  }

  int expectedRouteIndex() {
    return step.target == null ? -1 : route.indexOf(step.target);
  }

  String phaseName() {
    return phase.name();
  }

  String methodName() {
    return config.method().toString();
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

  int getPermanentShopProgress() {
    return Math.min(1150, rewardPoints + config.permanentPointsSpent());
  }

  int getFullSetsToPermanentBuyout() {
    return (Math.max(0, 1150 - getPermanentShopProgress()) + 34) / 35;
  }

  int getBatchesPerFullSet() {
    return (100 + config.method().plants - 1) / config.method().plants;
  }

  private void updateRewardPoints() {
    int current = client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS);
    if (lastRewardPoints >= 0 && current > lastRewardPoints)
      sessionPointsEarned += current - lastRewardPoints;
    rewardPoints = current;
    lastRewardPoints = current;
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

  int getSeedItemId() {
    return seedId();
  }

  boolean isInTitheFarm() {
    Player player = client.getLocalPlayer();
    WorldView worldView = client.getTopLevelWorldView();
    if (player == null || worldView == null || worldView.getScene() == null) {
      return false;
    }

    WorldPoint templateLocation =
        WorldPoint.fromLocalInstance(
            worldView.getScene(), player.getLocalLocation(), worldView.getPlane());
    return templateLocation != null && templateLocation.getRegionID() == TITHE_FARM_REGION;
  }

  int getWaterDoseCount() {
    return waterDoseCount();
  }

  boolean isBatchBlockedByWater() {
    if (route.isEmpty()) return false;
    boolean active =
        route.stream()
            .anyMatch(p -> plot(p).state != PlantState.EMPTY && plot(p).state != PlantState.DEAD);
    return !active && waterDoseCount() < config.method().plants * 3;
  }

  boolean isUsingGricollersCan() {
    return hasGricollersCan();
  }

  int getRemainingWaterNeeded() {
    if (route.isEmpty()) return config.method().plants * 3;
    int needed = 0;
    for (WorldPoint p : route) {
      TithePlot plot = plot(p);
      if (plot.state == PlantState.EMPTY && phase == Phase.PLANTING) needed += 3;
      else if (plot.state == PlantState.DRY || plot.state == PlantState.WATERED)
        needed += Math.max(0, 3 - plot.wateringCount);
    }
    return needed;
  }

  private boolean hasGricollersCan() {
    return hasItem(ItemID.ZEAH_WATERINGCAN);
  }

  private int firstWateringCan() {
    int[] ids = {
      ItemID.ZEAH_WATERINGCAN,
      ItemID.WATERING_CAN_8,
      ItemID.WATERING_CAN_7,
      ItemID.WATERING_CAN_6,
      ItemID.WATERING_CAN_5,
      ItemID.WATERING_CAN_4,
      ItemID.WATERING_CAN_3,
      ItemID.WATERING_CAN_2,
      ItemID.WATERING_CAN_1
    };
    for (int id : ids) if (hasItem(id)) return id;
    return ItemID.WATERING_CAN_8;
  }

  private int waterDoseCount() {
    if (hasGricollersCan())
      return Math.max(0, client.getVarbitValue(VarbitID.ZEAH_WATERINGCAN_CHARGES));
    ItemContainer inv = client.getItemContainer(InventoryID.INV);
    if (inv == null) return 0;
    return inv.count(ItemID.WATERING_CAN_1)
        + 2 * inv.count(ItemID.WATERING_CAN_2)
        + 3 * inv.count(ItemID.WATERING_CAN_3)
        + 4 * inv.count(ItemID.WATERING_CAN_4)
        + 5 * inv.count(ItemID.WATERING_CAN_5)
        + 6 * inv.count(ItemID.WATERING_CAN_6)
        + 7 * inv.count(ItemID.WATERING_CAN_7)
        + 8 * inv.count(ItemID.WATERING_CAN_8);
  }

  private void resetScene() {
    plots.clear();
    route.clear();
    depositSack = null;
    waterSource = null;
    phase = Phase.PLANTING;
    lastCompletedRouteIndex = -1;
    setPointBaseline = -1;
  }

  private void reset() {
    resetScene();
    harvestedTotal = 0;
  }
}
