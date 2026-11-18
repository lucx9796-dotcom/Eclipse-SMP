package com.eclipsesmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Set;

public class PluginHideListener implements Listener {

    private static final Set<String> NAMESPACES = Set.of("bukkit", "minecraft", "paper", "spigot");
    private final EclipseSMP plugin;

    public PluginHideListener(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("hide-plugins.enabled", true)) return;
        if (e.getPlayer().hasPermission("eclipse.plugins.bypass")) return;

        String msg = e.getMessage().trim();
        if (msg.startsWith("/")) msg = msg.substring(1);
        String label = msg.split("\\s+", 2)[0].toLowerCase();
        int colon = label.indexOf(':');
        if (colon > 0 && NAMESPACES.contains(label.substring(0, colon))) label = label.substring(colon + 1);

        List<String> hidden = plugin.getConfig().getStringList("hide-plugins.commands");
        if (!hidden.contains(label)) return;

        e.setCancelled(true);
        String name = plugin.getConfig().getString("hide-plugins.display-name", "Eclipse SMP");
        e.getPlayer().sendMessage(Component.text("Plugins (1): ", NamedTextColor.WHITE)
                .append(Component.text(name, NamedTextColor.GREEN)));
    }
}
