package com.eclipsesmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NicknameCommand implements TabExecutor {

    private final EclipseSMP plugin;

    public NicknameCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(plugin.msg("usage", "usage", "/nickname <player> <apelido|reset>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.msg("only-online"));
            return true;
        }
        String shown = plugin.identity().realName(target);

        if (args[1].equalsIgnoreCase("reset")) {
            plugin.identity().setNick(target, null);
            sender.sendMessage(plugin.msg("nick-reset", "player", shown));
            return true;
        }
        if (!args[1].matches("[A-Za-z0-9_]{3,16}")) {
            sender.sendMessage(plugin.msg("nick-invalid"));
            return true;
        }
        plugin.identity().setNick(target, args[1]);
        sender.sendMessage(plugin.msg("nick-set", "player", shown, "nick", args[1]));
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
