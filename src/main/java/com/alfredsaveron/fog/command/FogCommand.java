package com.alfredsaveron.fog.command;

import com.alfredsaveron.fog.manager.ConfigManager;
import com.alfredsaveron.fog.manager.SchedulerManager;
import com.alfredsaveron.fog.model.DepthZone;
import com.alfredsaveron.fog.model.PlayerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FogCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("on", "off", "reload", "status", "time");
    private static final List<String> ZONES = Arrays.asList("shallow", "deep", "abyss");

    private final SchedulerManager schedulerManager;
    private final ConfigManager configManager;

    public FogCommand(SchedulerManager schedulerManager, ConfigManager configManager) {
        this.schedulerManager = schedulerManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "on" -> handleOn(sender);
            case "off" -> handleOff(sender);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "time" -> handleTime(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleOn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "Only players can toggle FOG.");
            return;
        }

        PlayerState state = schedulerManager.getPlayerState(player.getUniqueId());
        if (state != null && state.isEnabled()) {
            sendError(sender, "fog is already enabled.");
            return;
        }

        schedulerManager.setEnabled(player.getUniqueId(), true);
        sendSuccess(sender, "FOG enabled. Descend into the depths.");
    }

    private void handleOff(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "Only players can toggle FOG.");
            return;
        }

        PlayerState state = schedulerManager.getPlayerState(player.getUniqueId());
        if (state != null && !state.isEnabled()) {
            sendError(sender, "fog is already disabled.");
            return;
        }

        schedulerManager.setEnabled(player.getUniqueId(), false);
        sendSuccess(sender, "FOG disabled. The silence returns.");
    }

    private void handleReload(CommandSender sender) {
        configManager.reload();
        sendSuccess(sender, "Configuration reloaded. "
                + configManager.getTotalAtmospheresLoaded() + " atmosphere(s) loaded.");
    }

    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendInfo(sender, "FOG is active with " + configManager.getTotalAtmospheresLoaded() + " atmosphere(s).");
            return;
        }

        PlayerState state = schedulerManager.getPlayerState(player.getUniqueId());
        if (state == null) {
            sendError(sender, "No state found. Try reconnecting.");
            return;
        }

        int y = player.getLocation().getBlockY();
        DepthZone zone = configManager.getZoneForY(y);
        boolean enabled = state.isEnabled();

        TextColor fColor = TextColor.color(0x31, 0x1D, 0x3F);
        TextColor oColor = TextColor.color(0x88, 0x1A, 0x30);
        TextColor gColor = TextColor.color(0xE2, 0x3E, 0x57);
        TextColor subtleGray = TextColor.color(0x4B, 0x55, 0x63);
        TextColor darkGray = TextColor.color(0x37, 0x41, 0x51);
        TextColor lightGray = TextColor.color(0x9C, 0xA3, 0xAF);

        Component fogStatusTitle = Component.text()
                .append(Component.text("  "))
                .append(Component.text("f", fColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("o", oColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("g", gColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text("  —  ", darkGray))
                .append(Component.text("status", subtleGray).decoration(TextDecoration.ITALIC, true))
                .build();

        sender.sendMessage(Component.empty());
        sender.sendMessage(fogStatusTitle);
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  enabled: ", lightGray)
                .append(Component.text(enabled ? "yes" : "no",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("  depth: ", lightGray)
                .append(Component.text("y = " + y, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  zone: ", lightGray)
                .append(Component.text(zone.getConfigKey(), zoneColor(zone))
                        .decoration(TextDecoration.BOLD, true)));
        sender.sendMessage(Component.text("  interval: ", lightGray)
                .append(Component.text(
                        zone == DepthZone.SURFACE ? "—" : configManager.getIntervalForZone(zone) + "s",
                        NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  atmospheres: ", lightGray)
                .append(Component.text(String.valueOf(configManager.getTotalAtmospheresLoaded()),
                        NamedTextColor.WHITE)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  ---------------------------------------------", darkGray));
        sender.sendMessage(Component.empty());
    }

    private void handleTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "usage: /fog time <shallow|deep|abyss> <seconds>");
            return;
        }

        String zoneStr = args[1].toLowerCase();
        DepthZone zone;
        try {
            zone = DepthZone.valueOf(zoneStr.toUpperCase());
            if (zone == DepthZone.SURFACE) {
                sendError(sender, "cannot set interval for surface zone.");
                return;
            }
        } catch (IllegalArgumentException e) {
            sendError(sender, "unknown depth zone: " + zoneStr);
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
            if (seconds <= 0) {
                sendError(sender, "seconds must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(sender, "seconds must be a positive integer.");
            return;
        }

        configManager.setInterval(zone, seconds);
        sendSuccess(sender, "interval for " + zone.getConfigKey() + " set to " + seconds + " seconds.");
    }

    private void sendUsage(CommandSender sender) {
        TextColor fColor = TextColor.color(0x31, 0x1D, 0x3F);
        TextColor oColor = TextColor.color(0x88, 0x1A, 0x30);
        TextColor gColor = TextColor.color(0xE2, 0x3E, 0x57);
        TextColor subtleGray = TextColor.color(0x4B, 0x55, 0x63);
        TextColor darkGray = TextColor.color(0x37, 0x41, 0x51);
        TextColor lightGray = TextColor.color(0x9C, 0xA3, 0xAF);

        Component fogTitle = Component.text()
                .append(Component.text("  "))
                .append(Component.text("f", fColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("o", oColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("g", gColor).decoration(TextDecoration.BOLD, true))
                .append(Component.text("  —  ", darkGray))
                .append(Component.text("you shouldn't have come down here.", subtleGray).decoration(TextDecoration.ITALIC, true))
                .build();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("        . . . the air grows heavy . . .", darkGray).decoration(TextDecoration.ITALIC, true));
        sender.sendMessage(Component.empty());
        sender.sendMessage(fogTitle);
        sender.sendMessage(Component.empty());
        
        sender.sendMessage(Component.text("  /fog on", lightGray)
                .append(Component.text("      . . . invite the silence.", subtleGray).decoration(TextDecoration.ITALIC, true)));
        
        sender.sendMessage(Component.text("  /fog off", lightGray)
                .append(Component.text("     . . . run back to the sun.", subtleGray).decoration(TextDecoration.ITALIC, true)));
        
        sender.sendMessage(Component.text("  /fog time", lightGray)
                .append(Component.text("    . . . delay the whispers.", subtleGray).decoration(TextDecoration.ITALIC, true)));

        sender.sendMessage(Component.text("  /fog status", lightGray)
                .append(Component.text("  . . . how deep are you?", subtleGray).decoration(TextDecoration.ITALIC, true)));
                
        sender.sendMessage(Component.text("  /fog reload", lightGray)
                .append(Component.text("  . . . reshape the whispers.", subtleGray).decoration(TextDecoration.ITALIC, true)));
                
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  ---------------------------------------------", darkGray));
        sender.sendMessage(Component.empty());
    }

    private void sendSuccess(CommandSender sender, String msg) {
        TextColor fColor = TextColor.color(0x31, 0x1D, 0x3F);
        TextColor oColor = TextColor.color(0x88, 0x1A, 0x30);
        TextColor gColor = TextColor.color(0xE2, 0x3E, 0x57);
        TextColor gray = TextColor.color(0x9C, 0xA3, 0xAF);

        Component prefix = Component.text()
                .append(Component.text("f", fColor))
                .append(Component.text("o", oColor))
                .append(Component.text("g", gColor))
                .append(Component.text(" » ", TextColor.color(0x4B, 0x55, 0x63)))
                .build();

        String formattedMsg = "whoop, " + msg.toLowerCase();
        sender.sendMessage(prefix.append(Component.text(formattedMsg, gray)));
    }

    private void sendError(CommandSender sender, String msg) {
        TextColor fColor = TextColor.color(0x31, 0x1D, 0x3F);
        TextColor oColor = TextColor.color(0x88, 0x1A, 0x30);
        TextColor gColor = TextColor.color(0xE2, 0x3E, 0x57);
        TextColor gray = TextColor.color(0x9C, 0xA3, 0xAF);

        Component prefix = Component.text()
                .append(Component.text("f", fColor))
                .append(Component.text("o", oColor))
                .append(Component.text("g", gColor))
                .append(Component.text(" » ", TextColor.color(0x4B, 0x55, 0x63)))
                .build();

        String formattedMsg = "oops, " + msg.toLowerCase();
        sender.sendMessage(prefix.append(Component.text(formattedMsg, gray)));
    }

    private void sendInfo(CommandSender sender, String msg) {
        TextColor gray = TextColor.color(0x6B, 0x72, 0x80);
        Component prefix = Component.text("  » ", TextColor.color(0x37, 0x41, 0x51));

        String formattedMsg = "psst, " + msg.toLowerCase();
        sender.sendMessage(prefix.append(Component.text(formattedMsg, gray).decoration(TextDecoration.ITALIC, true)));
    }

    private NamedTextColor zoneColor(DepthZone zone) {
        return switch (zone) {
            case SURFACE -> NamedTextColor.GREEN;
            case SHALLOW -> NamedTextColor.YELLOW;
            case DEEP -> NamedTextColor.GOLD;
            case ABYSS -> NamedTextColor.DARK_RED;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("time")) {
            return ZONES.stream()
                    .filter(z -> z.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("time")) {
            return Arrays.asList("5", "10", "15", "30", "60").stream()
                    .filter(t -> t.startsWith(args[2]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
