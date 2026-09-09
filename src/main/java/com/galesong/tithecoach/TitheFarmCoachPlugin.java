package com.galesong.tithecoach;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
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

  @Inject private Client client;
  @Inject private TitheFarmCoachConfig config;
  @Inject private OverlayManager overlayManager;
  @Inject private TitheFarmCoachOverlay overlay;
  @Inject private TitheFarmCoachPanel panel;

  private final CoachEngine coach = new CoachEngine();
  private GameObject depositSack;
  private GameObject waterSource;

  Map<WorldPoint, TithePlot> getPlots() {
    return coach.plots;
  }

  List<WorldPoint> getRoute() {
    return coach.route;
  }

  CoachStep getStep() {
    return coach.step;
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
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_1_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_1_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_1_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_2_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_2_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_2_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_3_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_3_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_3_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_4, PlantState.GROWN);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_A_4_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_1_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_1_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_1_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_2_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_2_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_2_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_3_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_3_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_3_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_4, PlantState.GROWN);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_B_4_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_1_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_1_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_1_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_2_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_2_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_2_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_3_DRY, PlantState.DRY);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_3_WET, PlantState.WATERED);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_3_DEAD, PlantState.DEAD);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_4, PlantState.GROWN);
    PLANT_IDS.put(ObjectID.HOSIDIUS_TITHE_C_4_DEAD, PlantState.DEAD);
  }

  @Provides
  TitheFarmCoachConfig provideConfig(ConfigManager manager) {
    return manager.getConfig(TitheFarmCoachConfig.class);
  }

  @Override
  protected void startUp() {
    coach.changeMethod(config.method());
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
      coach.changeMethod(config.method());
    }
  }

  @Subscribe
  public void onGameObjectSpawned(GameObjectSpawned e) {
    GameObject object = e.getGameObject();
    PlantState state = PLANT_IDS.get(object.getId());
    if (state != null) coach.updatePlot(object.getWorldLocation(), object, state, Instant.now());
    ObjectComposition composition = client.getObjectDefinition(object.getId());
    if (composition != null && composition.getActions() != null) {
      for (String action : composition.getActions()) {
        if (action == null) continue;
        if (action.equalsIgnoreCase("Deposit")) depositSack = object;
        if (action.equalsIgnoreCase("Fill")) waterSource = object;
      }
    }
  }

  @Subscribe
  public void onGameTick(GameTick ignored) {
    coach.updateRewardPoints(client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_REWARDPOINTS));
    if (!isInTitheFarm()) {
      if (!coach.plots.isEmpty() || !coach.route.isEmpty()) reset();
      return;
    }

    coach.inventory = inventory();
    if (coach.plots.size() < TitheRoutes.PATCH_COUNT) {
      coach.step = coach.preparationStep();
      return;
    }

    coach.beginSet();
    if (coach.route.isEmpty()) buildRoute();
    if (coach.route.isEmpty()) return;
    coach.depositSack = depositSack == null ? null : depositSack.getWorldLocation();
    coach.waterSource = waterSource == null ? null : waterSource.getWorldLocation();
    CoachStep next = coach.decide();
    if (next == null) {
      buildRoute();
      next = coach.decide();
    }

    coach.step = next;
  }

  private void buildRoute() {
    coach.route.addAll(
        TitheRoutes.build(client, config.method(), coach.plots, coach.getHarvestedTotal()));
  }

  private TitheInventory inventory() {
    ItemContainer items = client.getItemContainer(InventoryID.INV);
    return new TitheInventory(
        items == null ? null : items.getItems(),
        client.getVarbitValue(VarbitID.ZEAH_WATERINGCAN_CHARGES));
  }

  private void reset() {
    coach.reset();
    depositSack = null;
    waterSource = null;
  }

  int inventoryFruitCount() {
    return inventory().fruitCount();
  }

  int expectedRouteIndex() {
    return coach.step.target == null ? -1 : coach.route.indexOf(coach.step.target);
  }

  String methodName() {
    return config.method().toString();
  }

  int getSeedItemId() {
    return inventory().seedId();
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
    return templateLocation != null
        && templateLocation.getRegionID() == TitheRoutes.TITHE_FARM_REGION;
  }

  int getWaterDoseCount() {
    return inventory().waterDoseCount();
  }

  boolean isUsingGricollersCan() {
    return inventory().hasGricollersCan();
  }

  int getHarvestedTotal() {
    return coach.getHarvestedTotal();
  }

  int getRewardPoints() {
    return coach.getRewardPoints();
  }

  int getSessionPointsEarned() {
    return coach.getSessionPointsEarned();
  }

  int getCurrentSetPointsEarned() {
    return coach.getCurrentSetPointsEarned();
  }

  int getPermanentShopProgress() {
    return coach.getPermanentShopProgress(config.permanentPointsSpent());
  }

  int getFullSetsToPermanentBuyout() {
    return coach.getFullSetsToPermanentBuyout(config.permanentPointsSpent());
  }

  int getBatchesPerFullSet() {
    return coach.getBatchesPerFullSet();
  }

  String phaseName() {
    return coach.phaseName();
  }

  WorldPoint getQueuedPlantTarget() {
    return coach.getQueuedPlantTarget();
  }

  boolean isQueuedPlantClickReady() {
    return coach.isQueuedPlantClickReady();
  }

  int getRemainingWaterNeeded() {
    return coach.getRemainingWaterNeeded();
  }

  boolean isBatchBlockedByWater() {
    return coach.isBatchBlockedByWater(inventory().waterDoseCount());
  }
}
