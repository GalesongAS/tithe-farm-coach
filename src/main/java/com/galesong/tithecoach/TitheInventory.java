package com.galesong.tithecoach;

import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;

final class TitheInventory {
  private final Item[] items;
  private final int gricollerCharges;

  TitheInventory(Item[] items, int gricollerCharges) {
    this.items = items;
    this.gricollerCharges = gricollerCharges;
  }

  private int count(int id) {
    int quantity = 0;
    if (items != null) {
      for (Item item : items) if (item.getId() == id) quantity += item.getQuantity();
    }
    return quantity;
  }

  boolean hasItem(int id) {
    return count(id) > 0;
  }

  boolean hasSeed() {
    return seedId() != -1;
  }

  int seedId() {
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_C)) return ItemID.HOSIDIUS_TITHE_SEED_C;
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_B)) return ItemID.HOSIDIUS_TITHE_SEED_B;
    if (hasItem(ItemID.HOSIDIUS_TITHE_SEED_A)) return ItemID.HOSIDIUS_TITHE_SEED_A;
    return -1;
  }

  int fruitId() {
    if (hasItem(ItemID.HOSIDIUS_TITHE_FRUIT_C)) return ItemID.HOSIDIUS_TITHE_FRUIT_C;
    if (hasItem(ItemID.HOSIDIUS_TITHE_FRUIT_B)) return ItemID.HOSIDIUS_TITHE_FRUIT_B;
    return ItemID.HOSIDIUS_TITHE_FRUIT_A;
  }

  int fruitCount() {
    if (items == null) return 0;
    return count(ItemID.HOSIDIUS_TITHE_FRUIT_A)
        + count(ItemID.HOSIDIUS_TITHE_FRUIT_B)
        + count(ItemID.HOSIDIUS_TITHE_FRUIT_C);
  }

  boolean hasGricollersCan() {
    return hasItem(ItemID.ZEAH_WATERINGCAN);
  }

  int firstWateringCan() {
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

  int waterDoseCount() {
    if (hasGricollersCan()) return Math.max(0, gricollerCharges);
    if (items == null) return 0;
    return count(ItemID.WATERING_CAN_1)
        + 2 * count(ItemID.WATERING_CAN_2)
        + 3 * count(ItemID.WATERING_CAN_3)
        + 4 * count(ItemID.WATERING_CAN_4)
        + 5 * count(ItemID.WATERING_CAN_5)
        + 6 * count(ItemID.WATERING_CAN_6)
        + 7 * count(ItemID.WATERING_CAN_7)
        + 8 * count(ItemID.WATERING_CAN_8);
  }
}
