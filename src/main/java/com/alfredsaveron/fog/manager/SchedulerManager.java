package com.alfredsaveron.fog.manager;

import com.alfredsaveron.fog.model.DepthZone;
import com.alfredsaveron.fog.model.PlayerState;
import com.alfredsaveron.fog.model.Atmosphere;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SchedulerManager {

    private static final long CHECK_INTERVAL_TICKS = 20L;
    private static final int DARKNESS_DURATION_TICKS = 200;
    private static final int DARKNESS_AMPLIFIER = 0;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final SoundManager soundManager;

    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private BukkitTask mainTask;

    public SchedulerManager(JavaPlugin plugin, ConfigManager configManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.soundManager = soundManager;
    }

    public void start() {
        if (mainTask != null) return;

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        playerStates.clear();
    }

    public void addPlayer(UUID playerId) {
        playerStates.put(playerId, new PlayerState());
    }

    public void removePlayer(UUID playerId) {
        playerStates.remove(playerId);
    }

    public PlayerState getPlayerState(UUID playerId) {
        return playerStates.get(playerId);
    }

    public void setEnabled(UUID playerId, boolean enabled) {
        PlayerState state = playerStates.get(playerId);
        if (state != null) {
            state.setEnabled(enabled);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState state = playerStates.get(player.getUniqueId());
            if (state == null || !state.isEnabled()) continue;

            int y = player.getLocation().getBlockY();
            DepthZone zone = configManager.getZoneForY(y);

            DepthZone previousZone = state.getCurrentZone();
            state.setCurrentZone(zone);

            if (zone == DepthZone.SURFACE) continue;

            if (zone != previousZone) {
                state.clearShownMessages();
            }

            if (previousZone == DepthZone.SURFACE && zone != DepthZone.SURFACE) {
                playZoneTransition(player, zone);
                state.setLastEffectTimeMillis(now);
                continue;
            }

            if (zone.ordinal() > previousZone.ordinal() && previousZone != DepthZone.SURFACE) {
                playZoneTransition(player, zone);
                state.setLastEffectTimeMillis(now);
                continue;
            }

            int intervalSeconds = configManager.getIntervalForZone(zone);
            long intervalMillis = intervalSeconds * 1000L;

            if (now - state.getLastEffectTimeMillis() >= intervalMillis) {
                playEffect(player, zone);
                state.setLastEffectTimeMillis(now);
            }
        }
    }

    private void playEffect(Player player, DepthZone zone) {
        PlayerState state = playerStates.get(player.getUniqueId());
        Atmosphere atmosphere = configManager.getRandomAtmosphereForZone(zone);
        if (atmosphere == null) return;

        List<String> sounds = atmosphere.getSounds();

        String msg = pickUnseenMessage(atmosphere.getMessages(), state);
        if (msg != null) {
            sendChatMessage(player, msg);
            state.markMessageSeen(msg);

            if (zone == DepthZone.ABYSS) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS,
                        DARKNESS_DURATION_TICKS,
                        DARKNESS_AMPLIFIER,
                        false,
                        false,
                        false
                ));
            }
        }

        if (!sounds.isEmpty() && ThreadLocalRandom.current().nextFloat() < 0.6f) {
            String sound = sounds.get(ThreadLocalRandom.current().nextInt(sounds.size()));
            soundManager.playSound(player, sound);
        }
    }

    private String pickUnseenMessage(List<String> messages, PlayerState state) {
        List<String> unseen = new ArrayList<>();
        for (String msg : messages) {
            if (!state.hasSeenMessage(msg)) {
                unseen.add(msg);
            }
        }
        if (unseen.isEmpty()) return null;
        return unseen.get(ThreadLocalRandom.current().nextInt(unseen.size()));
    }

    private void playZoneTransition(Player player, DepthZone zone) {
        if (zone == DepthZone.ABYSS) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS,
                    DARKNESS_DURATION_TICKS,
                    DARKNESS_AMPLIFIER,
                    false, false, false
            ));
        }

        Atmosphere atmosphere = configManager.getRandomAtmosphereForZone(zone);
        if (atmosphere != null && !atmosphere.getSounds().isEmpty()) {
            String sound = atmosphere.getSounds().get(0);
            soundManager.playSound(player, sound);
        }
    }

    private void sendChatMessage(Player player, String text) {
        Component message = Component.text(text)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true);
        player.sendMessage(message);
    }
}
