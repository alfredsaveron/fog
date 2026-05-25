package com.alfredsaveron.fog;

import com.alfredsaveron.fog.listener.PlayerListener;
import com.alfredsaveron.fog.manager.CommandManager;
import com.alfredsaveron.fog.manager.ConfigManager;
import com.alfredsaveron.fog.manager.SchedulerManager;
import com.alfredsaveron.fog.manager.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FogPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SoundManager soundManager;
    private SchedulerManager schedulerManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        soundManager = new SoundManager();

        schedulerManager = new SchedulerManager(
                this, configManager, soundManager
        );

        commandManager = new CommandManager(this, schedulerManager, configManager);
        commandManager.registerCommands();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(schedulerManager), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            schedulerManager.addPlayer(player.getUniqueId());
        }

        schedulerManager.start();

        getLogger().info("  ☁ FOG — Atmosphere Engine enabled.");
        getLogger().info("  " + configManager.getTotalAtmospheresLoaded() + " atmosphere(s) loaded.");
    }

    @Override
    public void onDisable() {
        if (schedulerManager != null) {
            schedulerManager.stop();
        }
        getLogger().info("  ☁ FOG — Atmosphere Engine disabled.");
    }
}
