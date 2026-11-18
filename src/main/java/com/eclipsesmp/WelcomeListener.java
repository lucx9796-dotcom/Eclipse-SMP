package com.eclipsesmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class WelcomeListener implements Listener {

    private final EclipseSMP plugin;

    public WelcomeListener(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("welcome.enabled", true)) return;
        Player p = e.getPlayer();
        // 1s depois do login, para aparecer depois das mensagens de outros plugins
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            String minutes = String.valueOf(plugin.protection().minutes());
            for (String line : plugin.getConfig().getStringList("welcome.lines")) {
                p.sendMessage(plugin.legacy(line
                        .replace("{player}", p.getName())
                        .replace("{minutes}", minutes)));
            }
        }, 20L);
    }
}
