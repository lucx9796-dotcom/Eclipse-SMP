package com.eclipsesmp;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReviveCommand implements TabExecutor {

    private final EclipseSMP plugin;

    public ReviveCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.msg("usage", "usage", "/revive <player>"));
            return true;
        }
        String name = args[0];

        UUID id = plugin.data().deadByName(name);
        if (id == null) {
            OfflinePlayer op = plugin.findPlayer(name);
            if (op != null) id = op.getUniqueId();
        }

        String revived = id != null ? plugin.revive(id) : null;
        if (revived != null) {
            sender.sendMessage(plugin.msg("revived", "player", revived));
            org.bukkit.Bukkit.broadcast(plugin.msg("revived-broadcast", "player", revived));
        } else {
            plugin.pardon(name);
            sender.sendMessage(plugin.msg("revive-not-listed", "player", name));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String n : plugin.data().deadNames())
                if (n.toLowerCase().startsWith(args[0].toLowerCase())) out.add(n);
        }
        return out;
    }
}
