package com.galesong.tithecoach;

import java.time.Instant;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

final class TithePlot {
  static final int GROWTH_STAGE_SECONDS = 60;
  static final int URGENT_SECONDS_REMAINING = 15;

  final WorldPoint location;
  GameObject object;
  PlantState state;
  Instant stageStarted = Instant.EPOCH;
  int wateringCount = 0;

  TithePlot(WorldPoint location, GameObject object, PlantState state) {
    this.location = location;
    this.object = object;
    this.state = state;
  }
}
