package com.galesong.tithecoach;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Item;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class CoachEngineTest {
  private final CoachEngine coach = new CoachEngine();
  private final List<WorldPoint> patches = new ArrayList<>();
  private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

  public CoachEngineTest() {
    coach.changeMethod(TitheMethod.RELAXED_8);
    supplies(1000, 0);
    for (int i = 0; i < 8; i++) {
      WorldPoint point = new WorldPoint(1000 + i, 2000, 0);
      patches.add(point);
      coach.updatePlot(point, null, PlantState.EMPTY, now);
    }
    coach.route.addAll(patches);
  }

  @Test
  public void preparationChecksToolsSeedsAndWaterInOrder() {
    List<Item> items = new ArrayList<>();
    String[] expected = {
      "GET A SPADE", "GET A SEED DIBBER", "TAKE TITHE SEEDS", "FILL WATERING CANS"
    };
    int[] supplies = {
      ItemID.SPADE, ItemID.DIBBER, ItemID.HOSIDIUS_TITHE_SEED_A, ItemID.ZEAH_WATERINGCAN
    };
    for (int i = 0; i < supplies.length; i++) {
      coach.inventory = new TitheInventory(items.toArray(new Item[0]), 24);
      assertEquals(expected[i], coach.preparationStep().title);
      items.add(new Item(supplies[i], 1));
    }
    coach.inventory = new TitheInventory(items.toArray(new Item[0]), 24);
    assertEquals("ENTER TITHE FARM", coach.preparationStep().title);
  }

  @Test
  public void plantsThenWatersBeforeAdvancingAndQueuesTheNextPatch() {
    expect(CoachStep.Kind.PLANT, 0);
    change(0, PlantState.DRY);
    expect(CoachStep.Kind.WATER, 0);
    assertEquals(patches.get(1), coach.getQueuedPlantTarget());
    assertFalse(coach.isQueuedPlantClickReady());
    change(0, PlantState.WATERED);
    assertTrue(coach.isQueuedPlantClickReady());
    assertEquals(23, coach.getRemainingWaterNeeded());
    expect(CoachStep.Kind.PLANT, 1);
    assertNull(coach.getQueuedPlantTarget());
  }

  @Test
  public void finishesNewPlantPairBeforeReturningToOlderDryPlants() {
    change(0, PlantState.DRY);
    change(0, PlantState.WATERED);
    change(0, PlantState.DRY);
    expect(CoachStep.Kind.PLANT, 1);
    change(1, PlantState.DRY);
    expect(CoachStep.Kind.WATER, 1);
  }

  @Test
  public void wateringCircuitMovesForwardThenWrapsWithoutBacktracking() {
    plantBatch();
    expect(CoachStep.Kind.WAIT, 0);
    change(5, PlantState.DRY);
    change(5, PlantState.WATERED);
    change(0, PlantState.DRY);
    change(6, PlantState.DRY);
    expect(CoachStep.Kind.WATER, 6);
    change(6, PlantState.WATERED);
    expect(CoachStep.Kind.WATER, 0);
    change(0, PlantState.WATERED);
    expect(CoachStep.Kind.WAIT, 1);
  }

  @Test
  public void watersBeforeHarvestingButHarvestsRipePlantsBeforeWaiting() {
    plantBatch();
    expect(CoachStep.Kind.WAIT, 0);
    change(0, PlantState.GROWN);
    change(1, PlantState.DRY);
    expect(CoachStep.Kind.WATER, 1);
    change(1, PlantState.WATERED);
    expect(CoachStep.Kind.HARVEST, 0);
    change(0, PlantState.EMPTY);
    assertEquals(1, coach.getHarvestedTotal());
    expect(CoachStep.Kind.WAIT, 1);
  }

  @Test
  public void clearsDeadPlantsOnlyAfterLivingPlantsAreFinished() {
    plantBatch();
    expect(CoachStep.Kind.WAIT, 0);
    change(0, PlantState.DEAD);
    expect(CoachStep.Kind.WAIT, 1);
    for (int i = 1; i < 8; i++) {
      change(i, PlantState.GROWN);
      expect(CoachStep.Kind.HARVEST, i);
      change(i, PlantState.EMPTY);
    }
    expect(CoachStep.Kind.RECOVER, 0);
    change(0, PlantState.EMPTY);
    supplies(0, 7);
    expect(CoachStep.Kind.DEPOSIT, -1);
    assertEquals(7, coach.getHarvestedTotal());
  }

  @Test
  public void rejectsInsufficientStartingWaterAndRefillsDuringAnActiveBatch() {
    supplies(23, 0);
    expect(CoachStep.Kind.PREPARE, -1);
    assertTrue(coach.isBatchBlockedByWater());
    supplies(24, 0);
    expect(CoachStep.Kind.PLANT, 0);
    change(0, PlantState.DRY);
    supplies(0, 0);
    coach.waterSource = new WorldPoint(900, 2000, 0);
    assertEquals(coach.waterSource, decide().target);
    assertEquals(CoachStep.Kind.REFILL, coach.step.kind);
    supplies(1, 0);
    expect(CoachStep.Kind.WATER, 0);
  }

  @Test
  public void depositsBeforeRefillingThenStartsTheNextBatch() {
    finishBatch();
    supplies(0, 8);
    coach.depositSack = new WorldPoint(900, 2000, 0);
    assertEquals(CoachStep.Kind.DEPOSIT, decide().kind);
    assertEquals(coach.depositSack, coach.step.target);
    supplies(0, 0);
    assertEquals("REFILL GRICOLLER'S CAN", decide().title);
    supplies(1000, 0);
    assertNull(coach.decide());
    assertTrue(coach.route.isEmpty());
    coach.route.addAll(patches);
    expect(CoachStep.Kind.PLANT, 0);
  }

  @Test
  public void finalPartialBatchStillUsesFullMethodWaterCheckAndRefillsBeforeCompletion() {
    for (int batch = 0; batch < 12; batch++) {
      finishBatch();
      assertNull(coach.decide());
      coach.route.addAll(patches);
    }
    assertEquals(96, coach.getHarvestedTotal());
    coach.route.subList(4, 8).clear();
    supplies(12, 0);
    expect(CoachStep.Kind.PREPARE, -1);
    supplies(24, 0);
    finishBatch();
    assertEquals(100, coach.getHarvestedTotal());
    supplies(0, 4);
    expect(CoachStep.Kind.DEPOSIT, -1);
    supplies(0, 0);
    expect(CoachStep.Kind.REFILL, -1);
    supplies(24, 0);
    expect(CoachStep.Kind.COMPLETE, -1);
  }

  @Test
  public void methodChangeRetainsPlantsAndHarvestCountButRestartsPlanting() {
    plantBatch();
    expect(CoachStep.Kind.WAIT, 0);
    change(0, PlantState.GROWN);
    change(0, PlantState.EMPTY);
    coach.changeMethod(TitheMethod.SAFE_16);
    assertTrue(coach.route.isEmpty());
    assertEquals(1, coach.getHarvestedTotal());
    assertEquals(PlantState.WATERED, coach.plots.get(patches.get(1)).state);
    coach.route.addAll(patches);
    expect(CoachStep.Kind.PLANT, 0);
    assertEquals(48, coach.requiredWaterDoses());
  }

  @Test
  public void resetClearsRunButPreservesLaunchRewardEarnings() {
    coach.updateRewardPoints(100);
    coach.beginSet();
    coach.updateRewardPoints(135);
    finishBatch();
    coach.reset();
    assertTrue(coach.plots.isEmpty());
    assertTrue(coach.route.isEmpty());
    assertEquals(0, coach.getHarvestedTotal());
    assertEquals(35, coach.getSessionPointsEarned());
    coach.beginSet();
    assertEquals(0, coach.getCurrentSetPointsEarned());
  }

  @Test
  public void rewardSpendingDoesNotSubtractEarnedPointsAndBuyoutRoundsUp() {
    coach.updateRewardPoints(100);
    coach.beginSet();
    coach.updateRewardPoints(135);
    coach.updateRewardPoints(85);
    assertEquals(35, coach.getCurrentSetPointsEarned());
    assertEquals(135, coach.getPermanentShopProgress(50));
    assertEquals(29, coach.getFullSetsToPermanentBuyout(50));
    coach.updateRewardPoints(134);
    assertEquals(1, coach.getFullSetsToPermanentBuyout(1000));
    assertEquals(1150, coach.getPermanentShopProgress(1150));
    assertEquals(0, coach.getFullSetsToPermanentBuyout(1150));
  }

  @Test
  public void stageTimingResetsOnDryingButNotWateringAndHarvestsCountOnce() {
    change(0, PlantState.DRY);
    coach.updatePlot(patches.get(0), null, PlantState.WATERED, now.plusSeconds(5));
    assertEquals(now, coach.plots.get(patches.get(0)).stageStarted);
    coach.updatePlot(patches.get(0), null, PlantState.DRY, now.plusSeconds(60));
    assertEquals(now.plusSeconds(60), coach.plots.get(patches.get(0)).stageStarted);
    change(0, PlantState.GROWN);
    change(0, PlantState.EMPTY);
    change(0, PlantState.EMPTY);
    assertEquals(1, coach.getHarvestedTotal());
    assertEquals(0, coach.plots.get(patches.get(0)).wateringCount);
  }

  private CoachStep decide() {
    coach.step = coach.decide();
    return coach.step;
  }

  private void expect(CoachStep.Kind kind, int patch) {
    CoachStep step = decide();
    assertNotNull(step);
    assertEquals(kind, step.kind);
    assertEquals(patch < 0 ? null : patches.get(patch), step.target);
  }

  private void change(int index, PlantState state) {
    coach.updatePlot(patches.get(index), null, state, now);
  }

  private void plantBatch() {
    for (int i = 0; i < coach.route.size(); i++) {
      change(i, PlantState.DRY);
      change(i, PlantState.WATERED);
    }
  }

  private void finishBatch() {
    plantBatch();
    expect(CoachStep.Kind.WAIT, 0);
    for (int i = 0; i < coach.route.size(); i++) {
      change(i, PlantState.GROWN);
      expect(CoachStep.Kind.HARVEST, i);
      change(i, PlantState.EMPTY);
    }
  }

  private void supplies(int water, int fruit) {
    coach.inventory =
        new TitheInventory(
            new Item[] {
              new Item(ItemID.ZEAH_WATERINGCAN, 1),
              new Item(ItemID.HOSIDIUS_TITHE_SEED_A, 100),
              new Item(ItemID.HOSIDIUS_TITHE_FRUIT_A, fruit)
            },
            water);
  }
}
