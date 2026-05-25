package com.alfredsaveron.fog.manager;

import com.alfredsaveron.fog.command.FogCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandManager {

    private final JavaPlugin plugin;
    private final SchedulerManager schedulerManager;
    private final ConfigManager configManager;

    public CommandManager(JavaPlugin plugin, SchedulerManager schedulerManager,
                          ConfigManager configManager) {
        this.plugin = plugin;
        this.schedulerManager = schedulerManager;
        this.configManager = configManager;
    }

    public void registerCommands() {
        PluginCommand fogCommand = plugin.getCommand("fog");
        if (fogCommand == null) {
            plugin.getLogger().severe("Command 'fog' is not defined in plugin.yml!");
            return;
        }

        FogCommand executor = new FogCommand(schedulerManager, configManager);
        fogCommand.setExecutor(executor);
        fogCommand.setTabCompleter(executor);
    }
}
