package com.eclipsesmp;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImmortalCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("set", "unset", "toggle", "list", "reload");
    private final EclipseSMP plugin;

    public ImmortalCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage", "usage", "/immortal <player|set|unset|toggle|list|reload>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> { list(sender); return true; }
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(plugin.msg("reloaded"));
                return true;
            }
            case "set", "unset", "toggle" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("usage", "usage", "/immortal " + sub + " <player>"));
                    return true;
                }
                change(sender, args[1], sub);
                return true;
            }
            default -> { change(sender, args[0], "toggle"); return true; }
        }
    }

    private void change(CommandSender sender, String name, String mode) {
        OfflinePlayer target = plugin.findPlayer(name);
        if (target == null) {
            sender.sendMessage(plugin.msg("no-player"));
            return;
        }
        UUID id = target.getUniqueId();
        boolean now = switch (mode) {
            case "set" -> true;
            case "unset" -> false;
            default -> !plugin.data().isImmortal(id);
        };
        plugin.data().setImmortal(id, now);
        String shown = target.getName() != null ? target.getName() : name;
        sender.sendMessage(plugin.msg(now ? "immortal-on" : "immortal-off", "player", shown));
    }

    private void list(CommandSender sender) {
        if (plugin.data().immortals().isEmpty()) {
            sender.sendMessage(plugin.msg("immortal-list-empty"));
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : plugin.data().immortals()) {
            String n = Bukkit.getOfflinePlayer(id).getName();
            names.add(n != null ? n : id.toString());
        }
        sender.sendMessage(plugin.msg("immortal-list",
                "count", String.valueOf(names.size()), "players", String.join(", ", names)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : SUBS) if (s.startsWith(p)) out.add(s);
            for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        } else if (args.length == 2 && List.of("set", "unset", "toggle").contains(args[0].toLowerCase())) {
            String p = args[1].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        }
        return out;
    }
}
