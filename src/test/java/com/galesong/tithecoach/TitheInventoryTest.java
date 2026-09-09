package com.galesong.tithecoach;

import static org.junit.Assert.*;

import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class TitheInventoryTest {
  @Test
  public void sumsRegularCanDosesAndChoosesTheFullestCan() {
    TitheInventory inventory =
        new TitheInventory(
            new Item[] {new Item(ItemID.WATERING_CAN_8, 2), new Item(ItemID.WATERING_CAN_3, 1)},
            999);
    assertEquals(19, inventory.waterDoseCount());
    assertEquals(ItemID.WATERING_CAN_8, inventory.firstWateringCan());
  }

  @Test
  public void gricollersCanTakesPrecedenceOverRegularCansEvenWhenEmpty() {
    Item[] items = {new Item(ItemID.ZEAH_WATERINGCAN, 1), new Item(ItemID.WATERING_CAN_8, 1)};
    assertEquals(17, new TitheInventory(items, 17).waterDoseCount());
    assertEquals(0, new TitheInventory(items, -1).waterDoseCount());
    assertEquals(ItemID.ZEAH_WATERINGCAN, new TitheInventory(items, 0).firstWateringCan());
  }

  @Test
  public void selectsHighestSeedAndFruitTierWhileCountingAllFruit() {
    TitheInventory inventory =
        new TitheInventory(
            new Item[] {
              new Item(ItemID.HOSIDIUS_TITHE_SEED_A, 100),
              new Item(ItemID.HOSIDIUS_TITHE_SEED_C, 100),
              new Item(ItemID.HOSIDIUS_TITHE_FRUIT_A, 7),
              new Item(ItemID.HOSIDIUS_TITHE_FRUIT_B, 2)
            },
            0);
    assertEquals(ItemID.HOSIDIUS_TITHE_SEED_C, inventory.seedId());
    assertEquals(ItemID.HOSIDIUS_TITHE_FRUIT_B, inventory.fruitId());
    assertEquals(9, inventory.fruitCount());
    assertEquals(-1, new TitheInventory(null, 0).seedId());
    assertEquals(0, new TitheInventory(null, 0).waterDoseCount());
  }
}
