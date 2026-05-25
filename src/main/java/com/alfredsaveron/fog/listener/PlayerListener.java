package com.alfredsaveron.fog.listener;

import com.alfredsaveron.fog.manager.SchedulerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final SchedulerManager schedulerManager;

    public PlayerListener(SchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        schedulerManager.addPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        schedulerManager.removePlayer(event.getPlayer().getUniqueId());
    }
}
