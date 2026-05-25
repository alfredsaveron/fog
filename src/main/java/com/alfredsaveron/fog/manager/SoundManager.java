package com.alfredsaveron.fog.manager;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class SoundManager {

    private static final float VOLUME = 0.5f;
    private static final float MIN_PITCH = 0.8f;
    private static final float MAX_PITCH = 1.2f;

    public void playSound(Player player, String soundKey) {
        String resolved = resolveSoundKey(soundKey);
        float pitch = MIN_PITCH + ThreadLocalRandom.current().nextFloat() * (MAX_PITCH - MIN_PITCH);

        Sound sound = Sound.sound(
                Key.key(resolved),
                Sound.Source.AMBIENT,
                VOLUME,
                pitch
        );

        player.playSound(sound);
    }

    private String resolveSoundKey(String raw) {
        if (raw.contains(":")) {
            return raw;
        }
        return "minecraft:" + raw;
    }
}
