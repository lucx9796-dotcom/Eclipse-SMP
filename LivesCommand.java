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

public class LivesCommand implements TabExecutor {

    private static final List<String> ADMIN = List.of("set", "add", "remove", "reset");
    private final EclipseSMP plugin;

    public LivesCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    private int current(UUID id) {
        return Math.min(plugin.data().getLives(id, plugin.maxLives()), plugin.maxLives());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /lives  -> vidas de quem digitou
        if (args.length == 0) {
            if (sender instanceof Player p) {
                sender.sendMessage(plugin.msg("lives-self",
                        "lives", String.valueOf(current(p.getUniqueId())),
                        "max", String.valueOf(plugin.maxLives())));
            } else {
                sender.sendMessage(plugin.msg("usage", "usage", "/lives <player>"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // /lives <player> -> consulta
        if (!ADMIN.contains(sub)) {
            OfflinePlayer t = plugin.findPlayer(args[0]);
            if (t == null) {
                sender.sendMessage(plugin.msg("no-player"));
                return true;
            }
            String name = t.getName() != null ? t.getName() : args[0];
            sender.sendMessage(plugin.msg("lives-other", "player", name,
                    "lives", String.valueOf(current(t.getUniqueId())),
                    "max", String.valueOf(plugin.maxLives())));
            return true;
        }

        // set / add / remove / reset
        if (!sender.hasPermission("eclipse.lives.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        boolean needsNumber = !sub.equals("reset");
        if (args.length < (needsNumber ? 3 : 2)) {
            sender.sendMessage(plugin.msg("usage", "usage",
                    needsNumber ? "/lives " + sub + " <player> <número>" : "/lives reset <player>"));
            return true;
        }
        OfflinePlayer t = plugin.findPlayer(args[1]);
        if (t == null) {
            sender.sendMessage(plugin.msg("no-player"));
            return true;
        }
        UUID id = t.getUniqueId();
        String name = t.getName() != null ? t.getName() : args[1];
        int max = plugin.maxLives();

        if (sub.equals("reset")) {
            plugin.data().resetLives(id);
            sender.sendMessage(plugin.msg("lives-reset", "player", name, "max", String.valueOf(max)));
            return true;
        }

        int n;
        try {
            n = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.msg("usage", "usage", "/lives " + sub + " <player> <número>"));
            return true;
        }
        int result = switch (sub) {
            case "set" -> n;
            case "add" -> current(id) + n;
            default -> current(id) - n;
        };
        result = Math.max(1, Math.min(max, result)); // mínimo 1: para zerar, o jogador precisa morrer
        plugin.data().setLives(id, result);
        sender.sendMessage(plugin.msg("lives-set", "player", name, "lives", String.valueOf(result)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            if (sender.hasPermission("eclipse.lives.admin"))
                for (String s : ADMIN) if (s.startsWith(p)) out.add(s);
            for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        } else if (args.length == 2 && ADMIN.contains(args[0].toLowerCase())) {
            String p = args[1].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        }
        return out;
    }
}
