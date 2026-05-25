package com.alfredsaveron.fog.manager;

import com.alfredsaveron.fog.model.DepthZone;
import com.alfredsaveron.fog.model.Atmosphere;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    private int shallowThreshold = 59;
    private int deepThreshold = 29;
    private int abyssThreshold = -1;

    private final Map<DepthZone, Integer> intervals = new EnumMap<>(DepthZone.class);
    private final Map<DepthZone, List<Atmosphere>> atmospheresByZone = new EnumMap<>(DepthZone.class);

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        atmospheresByZone.clear();
        intervals.clear();

        for (DepthZone zone : DepthZone.values()) {
            atmospheresByZone.put(zone, new ArrayList<>());
        }

        ConfigurationSection depthSection = plugin.getConfig().getConfigurationSection("depth");
        if (depthSection != null) {
            shallowThreshold = depthSection.getInt("shallow", 59);
            deepThreshold = depthSection.getInt("deep", 29);
            abyssThreshold = depthSection.getInt("abyss", -1);
        }

        ConfigurationSection intervalSection = plugin.getConfig().getConfigurationSection("intervals");
        if (intervalSection != null) {
            intervals.put(DepthZone.SHALLOW, intervalSection.getInt("shallow", 120));
            intervals.put(DepthZone.DEEP, intervalSection.getInt("deep", 300));
            intervals.put(DepthZone.ABYSS, intervalSection.getInt("abyss", 600));
        } else {
            intervals.put(DepthZone.SHALLOW, 120);
            intervals.put(DepthZone.DEEP, 300);
            intervals.put(DepthZone.ABYSS, 600);
        }

        ConfigurationSection atmospheresSection = plugin.getConfig().getConfigurationSection("atmospheres");
        if (atmospheresSection == null) {
            logger.warning("No 'atmospheres' section found in config.yml!");
            return;
        }

        for (String key : atmospheresSection.getKeys(false)) {
            ConfigurationSection s = atmospheresSection.getConfigurationSection(key);
            if (s == null) continue;

            String zoneName = s.getString("zone", "shallow");
            List<String> messages = s.getStringList("messages");
            List<String> sounds = s.getStringList("sounds");

            if (messages.isEmpty()) {
                logger.warning("Atmosphere '" + key + "' has no messages — skipping.");
                continue;
            }

            Atmosphere atmosphere = new Atmosphere(messages, sounds);

            DepthZone zone = zoneFromString(zoneName);
            if (zone != null && zone != DepthZone.SURFACE) {
                atmospheresByZone.get(zone).add(atmosphere);
            }

            logger.info("Loaded atmosphere: " + key + " [" + zoneName + "] ("
                    + messages.size() + " messages, "
                    + sounds.size() + " sounds)");
        }

        logger.info("FOG loaded " + getTotalAtmospheresLoaded() + " atmosphere(s) across "
                + atmospheresByZone.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .count() + " zone(s).");
    }

    public void reload() {
        loadConfig();
    }

    public Atmosphere getRandomAtmosphereForZone(DepthZone zone) {
        List<Atmosphere> atmospheres = atmospheresByZone.get(zone);
        if (atmospheres == null || atmospheres.isEmpty()) return null;
        return atmospheres.get(ThreadLocalRandom.current().nextInt(atmospheres.size()));
    }

    public int getIntervalForZone(DepthZone zone) {
        return intervals.getOrDefault(zone, 10);
    }

    public void setInterval(DepthZone zone, int seconds) {
        intervals.put(zone, seconds);
        plugin.getConfig().set("intervals." + zone.getConfigKey(), seconds);
        plugin.saveConfig();
    }

    public DepthZone getZoneForY(int y) {
        return DepthZone.fromY(y, shallowThreshold, deepThreshold, abyssThreshold);
    }

    public int getTotalAtmospheresLoaded() {
        return atmospheresByZone.values().stream().mapToInt(List::size).sum();
    }

    private DepthZone zoneFromString(String name) {
        try {
            return DepthZone.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown zone: " + name);
            return null;
        }
    }
}
