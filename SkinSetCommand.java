package com.eclipsesmp;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkinSetCommand implements TabExecutor {

    private final EclipseSMP plugin;

    public SkinSetCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.msg("usage", "usage", "/skinset <player> <nome da skin|reset>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.msg("only-online"));
            return true;
        }
        String shown = plugin.identity().realName(target);
        UUID targetId = target.getUniqueId();

        if (args[1].equalsIgnoreCase("reset")) {
            plugin.identity().setSkin(target, null, null);
            sender.sendMessage(plugin.msg("skin-reset", "player", shown));
            return true;
        }

        String skinName = args[1];
        sender.sendMessage(plugin.msg("skin-loading", "skin", skinName));

        // Consulta à Mojang fora da thread principal
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ProfileProperty tex = null;
            try {
                PlayerProfile fetched = Bukkit.createProfile(skinName);
                if (fetched.complete(true)) {
                    for (ProfileProperty pr : fetched.getProperties()) {
                        if (pr.getName().equals("textures")) { tex = pr; break; }
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Falha ao buscar skin '" + skinName + "': " + ex.getMessage());
            }
            final ProfileProperty result = tex;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(targetId);
                if (result == null) {
                    sender.sendMessage(plugin.msg("skin-fail", "skin", skinName));
                } else if (online != null) {
                    plugin.identity().setSkin(online, skinName, result);
                    sender.sendMessage(plugin.msg("skin-set", "player", shown, "skin", skinName));
                } else {
                    sender.sendMessage(plugin.msg("only-online"));
                }
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
        } else if (args.length == 2 && "reset".startsWith(args[1].toLowerCase())) {
            out.add("reset");
        }
        return out;
    }
}
