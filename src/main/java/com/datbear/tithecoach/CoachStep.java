package com.datbear.tithecoach;

import net.runelite.api.coords.WorldPoint;

final class CoachStep {
  enum Kind {
    PREPARE,
    PLANT,
    WATER,
    WAIT,
    HARVEST,
    DEPOSIT,
    REFILL,
    COMPLETE,
    RECOVER
  }

  final Kind kind;
  final String title;
  final String detail;
  final WorldPoint target;
  final int inventoryItem;

  CoachStep(Kind kind, String title, String detail, WorldPoint target, int inventoryItem) {
    this.kind = kind;
    this.title = title;
    this.detail = detail;
    this.target = target;
    this.inventoryItem = inventoryItem;
  }

  String key() {
    return kind + ":" + (target == null ? "" : target.toString());
  }
}
