package com.eclipsesmp;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProtectionCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("give", "remove", "check");
    private final EclipseSMP plugin;

    public ProtectionCommand(EclipseSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 2 || !SUBS.contains(args[0].toLowerCase())) {
            sender.sendMessage(plugin.msg("usage", "usage", "/protection <give|remove|check> <player>"));
            return true;
        }
        OfflinePlayer target = plugin.findPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("no-player"));
            return true;
        }
        UUID id = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : args[1];
        ProtectionManager pm = plugin.protection();

        switch (args[0].toLowerCase()) {
            case "give" -> {
                pm.grant(id);
                sender.sendMessage(plugin.msg("protection-given", "player", name,
                        "minutes", String.valueOf(pm.minutes())));
            }
            case "remove" -> {
                pm.remove(id);
                sender.sendMessage(plugin.msg("protection-removed", "player", name));
            }
            default -> {
                if (pm.isProtected(id)) {
                    sender.sendMessage(plugin.msg("protection-status", "player", name,
                            "time", ProtectionManager.fmt(pm.remaining(id))));
                } else {
                    sender.sendMessage(plugin.msg("protection-none", "player", name));
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        }
        return out;
    }
}
