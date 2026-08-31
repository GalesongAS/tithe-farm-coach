package com.galesong.tithecoach;

import java.time.Instant;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

final class TithePlot {
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
