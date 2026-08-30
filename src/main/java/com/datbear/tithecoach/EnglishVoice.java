package com.datbear.tithecoach;

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EnglishVoice {
  private static final Logger LOG = LoggerFactory.getLogger(EnglishVoice.class);

  private final AudioPlayer player;

  EnglishVoice(AudioPlayer player) {
    this.player = player;
  }

  void speak(CoachStep.Kind kind) {
    String clip;
    switch (kind) {
      case PLANT:
        clip = "plant.wav";
        break;
      case WATER:
        clip = "water.wav";
        break;
      case HARVEST:
        clip = "harvest.wav";
        break;
      case DEPOSIT:
        clip = "deposit.wav";
        break;
      case REFILL:
        clip = "refill.wav";
        break;
      case COMPLETE:
        clip = "complete.wav";
        break;
      case RECOVER:
        clip = "recover.wav";
        break;
      default:
        clip = "prepare.wav";
    }
    try {
      player.play(EnglishVoice.class, "/voice/" + clip, 0f);
    } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
      LOG.debug("Unable to play Tithe Farm voice prompt {}", clip, ex);
    }
  }
}
