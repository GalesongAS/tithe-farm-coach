package com.galesong.tithecoach;

public enum TitheMethod {
  RELAXED_8("Relaxed 8 - most forgiving", 8),
  SAFE_16("Safe 16 - generous timing", 16),
  STANDARD_20("Standard 20x5 - moderate", 20),
  SIMPLE_23("Simple 23 - tight timing", 23),
  ADVANCED_25("Advanced 25x4 - tightest", 25);

  final String label;
  final int plants;

  TitheMethod(String label, int plants) {
    this.label = label;
    this.plants = plants;
  }

  @Override
  public String toString() {
    return label;
  }
}
